"""LLM Provider 工厂"""
from __future__ import annotations

from typing import Any, AsyncGenerator

import httpx

from app.core.config import Settings


# 禁用 keep-alive，避免 httpx 0.28 + httpcore 1.0 在 Windows 上对 localhost 偶发 502
# 同时所有 client 设置 trust_env=False，不读取系统代理（Windows 注册表 / env HTTP_PROXY），
# 避免本应直连本地 11434 的请求被发到 127.0.0.1:7897 等代理端口
_OLLAMA_LIMITS = httpx.Limits(max_keepalive_connections=0, max_connections=20)


class ProviderFactory:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def get_active_provider_summary(self) -> dict[str, Any]:
        if self.settings.providers.active_mode.lower() == "offline":
            ollama = self.settings.providers.offline.ollama
            return {
                "mode": "offline",
                "provider": self.settings.providers.offline.provider,
                "baseUrl": ollama.base_url,
                "chatPath": ollama.chat_path,
                "tagsPath": ollama.tags_path,
                "modelName": ollama.model_name,
                "temperature": self.settings.providers.defaults.temperature,
                "topP": self.settings.providers.defaults.top_p,
                "maxTokens": self.settings.providers.defaults.max_tokens,
            }

        online = self.settings.providers.online
        return {
            "mode": "online",
            "provider": online.provider,
            "baseUrl": online.base_url,
            "chatPath": online.chat_path,
            "modelName": online.model_name,
            "apiKeyConfigured": bool(online.api_key),
            "temperature": self.settings.providers.defaults.temperature,
            "topP": self.settings.providers.defaults.top_p,
            "maxTokens": self.settings.providers.defaults.max_tokens,
        }

    def _resolve_runtime(self, provider_override: dict[str, Any] | None = None) -> dict[str, Any]:
        override = provider_override or {}
        active_mode = str(override.get("activeMode") or self.settings.providers.active_mode or "offline").lower()
        defaults = override.get("defaults") if isinstance(override.get("defaults"), dict) else {}

        runtime = {
            "mode": active_mode,
            "temperature": defaults.get("temperature", self.settings.providers.defaults.temperature),
            "top_p": defaults.get("topP", defaults.get("top_p", self.settings.providers.defaults.top_p)),
            "max_tokens": defaults.get("maxTokens", defaults.get("max_tokens", self.settings.providers.defaults.max_tokens)),
        }

        if active_mode == "online":
            online_override = override.get("online") if isinstance(override.get("online"), dict) else {}
            runtime.update({
                "base_url": str(online_override.get("baseUrl") or self.settings.providers.online.base_url),
                "chat_path": str(online_override.get("chatPath") or self.settings.providers.online.chat_path),
                "model_name": str(online_override.get("modelName") or self.settings.providers.online.model_name),
                "api_key": str(online_override.get("apiKey") or self.settings.providers.online.api_key or ""),
            })
            return runtime

        offline_override = override.get("offline") if isinstance(override.get("offline"), dict) else {}
        ollama_override = offline_override.get("ollama") if isinstance(offline_override.get("ollama"), dict) else {}
        runtime.update({
            "base_url": str(ollama_override.get("baseUrl") or self.settings.providers.offline.ollama.base_url),
            "chat_path": str(ollama_override.get("chatPath") or self.settings.providers.offline.ollama.chat_path),
            "model_name": str(ollama_override.get("modelName") or self.settings.providers.offline.ollama.model_name),
        })
        return runtime

    async def list_models(self) -> dict[str, Any]:
        if self.settings.providers.active_mode.lower() != "offline":
            online = self.settings.providers.online
            return {
                "mode": "online",
                "models": [online.model_name],
                "message": "Online mode does not query a local model registry.",
            }

        ollama = self.settings.providers.offline.ollama
        url = f"{ollama.base_url.rstrip('/')}{ollama.tags_path}"

        async with httpx.AsyncClient(timeout=10.0, limits=_OLLAMA_LIMITS, trust_env=False) as client:
            response = await client.get(url)
            response.raise_for_status()
            payload = response.json()

        return {
            "mode": "offline",
            "provider": "ollama",
            "endpoint": url,
            "models": payload.get("models", []),
        }

    def generate(
        self,
        prompt: str,
        system_prompt: str = "",
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
        provider_override: dict[str, Any] | None = None,
    ) -> str:
        """同步生成回答"""
        import asyncio
        return asyncio.run(self.async_generate(prompt, system_prompt, temperature, max_tokens, top_p, provider_override))

    async def async_generate(
        self,
        prompt: str,
        system_prompt: str = "",
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
        provider_override: dict[str, Any] | None = None,
    ) -> str:
        """异步生成回答"""
        runtime = self._resolve_runtime(provider_override)
        if runtime["mode"] == "offline":
            return await self._generate_ollama(prompt, system_prompt, runtime, temperature, max_tokens, top_p)
        return await self._generate_online(prompt, system_prompt, runtime, temperature, max_tokens, top_p)

    async def _generate_ollama(
        self,
        prompt: str,
        system_prompt: str = "",
        runtime: dict[str, Any] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
    ) -> str:
        """调用 Ollama 生成回答"""
        runtime = runtime or self._resolve_runtime()
        url = f"{str(runtime['base_url']).rstrip('/')}{runtime['chat_path']}"

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        payload: dict[str, Any] = {
            "model": runtime["model_name"],
            "messages": messages,
            "stream": False,
        }
        options: dict[str, Any] = {}
        _t = temperature if temperature is not None else runtime.get("temperature")
        if _t is not None:
            options["temperature"] = _t
        _p = top_p if top_p is not None else runtime.get("top_p")
        if _p is not None:
            options["top_p"] = _p
        _m = max_tokens if max_tokens is not None else runtime.get("max_tokens")
        if _m is not None:
            options["max_tokens"] = _m
        if options:
            payload["options"] = options

        async with httpx.AsyncClient(timeout=60.0, limits=_OLLAMA_LIMITS, trust_env=False) as client:
            response = await client.post(url, json=payload)
            response.raise_for_status()
            result = response.json()

        return result.get("message", {}).get("content", "") or "生成失败"

    async def _generate_online(
        self,
        prompt: str,
        system_prompt: str = "",
        runtime: dict[str, Any] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
    ) -> str:
        """调用在线模型生成回答"""
        runtime = runtime or self._resolve_runtime({"activeMode": "online"})
        url = f"{str(runtime['base_url']).rstrip('/')}{runtime['chat_path']}"

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        headers = {}
        if runtime.get("api_key"):
            headers["Authorization"] = f"Bearer {runtime['api_key']}"

        payload: dict[str, Any] = {
            "model": runtime["model_name"],
            "messages": messages,
            "stream": False,
        }
        _t = temperature if temperature is not None else runtime.get("temperature")
        if _t is not None:
            payload["temperature"] = _t
        _p = top_p if top_p is not None else runtime.get("top_p")
        if _p is not None:
            payload["top_p"] = _p
        _m = max_tokens if max_tokens is not None else runtime.get("max_tokens")
        if _m is not None:
            payload["max_tokens"] = _m

        async with httpx.AsyncClient(timeout=60.0, limits=_OLLAMA_LIMITS, trust_env=False) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            result = response.json()

        choices = result.get("choices", [])
        if not choices:
            return "生成失败"

        return choices[0].get("message", {}).get("content", "") or "生成失败"

    async def stream_generate(
        self,
        prompt: str,
        system_prompt: str = "",
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
        provider_override: dict[str, Any] | None = None,
        ollama_images: list[str] | None = None,
        online_multimodal_content: list[dict[str, Any]] | None = None,
        messages: list[dict[str, Any]] | None = None,
        chat_template_kwargs: dict[str, Any] | None = None,
    ) -> AsyncGenerator[str, None]:
        """流式生成回答。
        <p>当传入 {@code messages} 时, 直接作为 LLM 的 messages 数组 (覆盖 prompt+system_prompt 拼接);
        否则走原逻辑 (仅 system + user(prompt))。
        <p>{@code chat_template_kwargs} 透传给 LLM 请求体 (Qwen3 等: {enable_thinking: false} 关闭思考).
        """
        runtime = self._resolve_runtime(provider_override)
        if runtime["mode"] == "offline":
            async for chunk in self._stream_generate_ollama(prompt, system_prompt, runtime, temperature, max_tokens, top_p, ollama_images, messages, chat_template_kwargs):
                yield chunk
        else:
            async for chunk in self._stream_generate_online(prompt, system_prompt, runtime, temperature, max_tokens, top_p, online_multimodal_content, messages, chat_template_kwargs):
                yield chunk

    async def _stream_generate_ollama(
        self,
        prompt: str,
        system_prompt: str = "",
        runtime: dict[str, Any] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
        ollama_images: list[str] | None = None,
        messages: list[dict[str, Any]] | None = None,
        chat_template_kwargs: dict[str, Any] | None = None,
    ) -> AsyncGenerator[str, None]:
        """流式调用 Ollama. 参数保留兼容但 Ollama 原生 options 不支持 enable_thinking, 仅在线模式生效."""
        runtime = runtime or self._resolve_runtime()
        url = f"{str(runtime['base_url']).rstrip('/')}{runtime['chat_path']}"

        # 优先使用调用方传入的 messages (多轮对话历史)
        if messages:
            payload_messages = list(messages)
            # 多模态: 若 caller 传了 messages 但又给了 ollama_images, 将 images 附加到最后一条 user 消息
            if ollama_images and payload_messages and payload_messages[-1].get("role") == "user":
                payload_messages[-1]["images"] = ollama_images
        else:
            payload_messages = []
            if system_prompt:
                payload_messages.append({"role": "system", "content": system_prompt})
            # Ollama 多模态：在 message 中使用 images 字段（纯 base64 数组）
            user_msg: dict[str, Any] = {"role": "user", "content": prompt}
            if ollama_images:
                user_msg["images"] = ollama_images
                import logging
                logging.getLogger(__name__).info(
                    f"[Ollama multimodal] model={runtime['model_name']}, images_count={len(ollama_images)}, first_img_len={len(ollama_images[0]) if ollama_images else 0}, has_newline={chr(10) in (ollama_images[0] if ollama_images else '')}"
                )
            payload_messages.append(user_msg)

        payload: dict[str, Any] = {
            "model": runtime["model_name"],
            "messages": payload_messages,
            "stream": True,
        }
        options: dict[str, Any] = {}
        _t = temperature if temperature is not None else runtime.get("temperature")
        if _t is not None:
            options["temperature"] = _t
        _p = top_p if top_p is not None else runtime.get("top_p")
        if _p is not None:
            options["top_p"] = _p
        _m = max_tokens if max_tokens is not None else runtime.get("max_tokens")
        if _m is not None:
            options["max_tokens"] = _m
        if options:
            payload["options"] = options

        async with httpx.AsyncClient(timeout=60.0, limits=_OLLAMA_LIMITS, trust_env=False) as client:
            async with client.stream("POST", url, json=payload) as response:
                response.raise_for_status()
                # 容错：检测 OCR/视觉模型的"循环输出"bug（如 glm-ocr 把同一段文字重复 N 次）
                # 维护一个最近输出的滑动窗口，若窗口内容已出现 >= 2 次，认为模型在循环，停止流式
                _seen_windows: set[str] = set()
                _window_buf: list[str] = []
                _last_window: str | None = None
                _repeat_count = 0
                async for line in response.aiter_lines():
                    line = (line or "").strip()
                    if not line:
                        continue
                    if line.startswith("data:"):
                        line = line[5:].strip()
                    if line == "[DONE]":
                        break
                    import json
                    try:
                        data = json.loads(line)
                    except Exception:
                        continue
                    chunk = data.get("message", {}).get("content", "")
                    if chunk:
                        # 滑动窗口去重：每 32 字符一窗，重复出现即终止
                        _window_buf.append(chunk)
                        _cur = "".join(_window_buf)
                        if len(_cur) >= 32:
                            tail = _cur[-32:]
                            if tail in _seen_windows:
                                _repeat_count += 1
                                if _repeat_count >= 2:
                                    break
                            else:
                                _seen_windows.add(tail)
                                _repeat_count = 0
                            _last_window = tail
                            if len(_window_buf) > 8:
                                _window_buf = _window_buf[-4:]
                        yield chunk
                    if bool(data.get("done")):
                        break

    async def _stream_generate_online(
        self,
        prompt: str,
        system_prompt: str = "",
        runtime: dict[str, Any] | None = None,
        temperature: float | None = None,
        max_tokens: int | None = None,
        top_p: float | None = None,
        online_multimodal_content: list[dict[str, Any]] | None = None,
        messages: list[dict[str, Any]] | None = None,
        chat_template_kwargs: dict[str, Any] | None = None,
    ) -> AsyncGenerator[str, None]:
        """流式调用在线模型. chat_template_kwargs 合并到请求体顶层 (Qwen3 等: {enable_thinking: false})."""
        runtime = runtime or self._resolve_runtime({"activeMode": "online"})
        url = f"{str(runtime['base_url']).rstrip('/')}{runtime['chat_path']}"

        # 优先使用调用方传入的 messages (多轮对话历史)
        if messages:
            payload_messages = list(messages)
            # 多模态: 若 caller 传了 messages 但又给了 online_multimodal_content, 追加到最后一条 user 消息
            if online_multimodal_content and payload_messages and payload_messages[-1].get("role") == "user":
                payload_messages[-1]["content"] = online_multimodal_content
        else:
            payload_messages = []
            if system_prompt:
                payload_messages.append({"role": "system", "content": system_prompt})
            # 在线模型多模态：使用 OpenAI content 数组格式
            if online_multimodal_content:
                payload_messages.append({"role": "user", "content": online_multimodal_content})
            else:
                payload_messages.append({"role": "user", "content": prompt})

        headers: dict[str, str] = {}
        if runtime.get("api_key"):
            headers["Authorization"] = f"Bearer {runtime['api_key']}"

        payload: dict[str, Any] = {
            "model": runtime["model_name"],
            "messages": payload_messages,
            "stream": True,
        }
        _t = temperature if temperature is not None else runtime.get("temperature")
        if _t is not None:
            payload["temperature"] = _t
        _p = top_p if top_p is not None else runtime.get("top_p")
        if _p is not None:
            payload["top_p"] = _p
        _m = max_tokens if max_tokens is not None else runtime.get("max_tokens")
        if _m is not None:
            payload["max_tokens"] = _m
        if chat_template_kwargs:
            payload.update(chat_template_kwargs)

        import json, logging
        _logger = logging.getLogger(__name__)
        _safe_payload = {k: (v if k != "messages" else f"[{len(v)} messages]") for k, v in payload.items()}
        _safe_payload["_url"] = url
        _logger.info(f"_stream_generate_online request: {json.dumps(_safe_payload)}")

        async with httpx.AsyncClient(timeout=120.0, limits=_OLLAMA_LIMITS, trust_env=False) as client:
            async with client.stream("POST", url, json=payload, headers=headers) as response:
                if response.status_code >= 400:
                    body = await response.aread()
                    _logger.error(f"LLM 400: url={url}, status={response.status_code}, body={body.decode('utf-8', errors='replace')[:1000]}")
                    _logger.error(f"LLM 400 payload keys: {list(payload.keys())}, model={payload.get('model')}")
                response.raise_for_status()
                # 调试: 记录 content-type, 确认服务端返回的格式
                _logger.info(f"_stream_generate_online response: status={response.status_code}, content_type={response.headers.get('content-type')}")
                _line_count = 0
                _chunk_count = 0
                _first_raw = None
                async for line in response.aiter_lines():
                    _line_count += 1
                    line = (line or "").strip()
                    if not line:
                        continue
                    if _first_raw is None:
                        _first_raw = line[:300]
                    if not line.startswith("data:"):
                        # 调试: 记录非 data: 行, 帮助定位是否服务端返回了非 SSE 格式
                        if not line.startswith(":"):
                            _logger.info(f"_stream_generate_online non-data line: {line[:200]}")
                        continue
                    raw = line[5:].strip()
                    if raw == "[DONE]":
                        _logger.info(f"_stream_generate_online: done received, total_lines={_line_count}, chunks_yielded={_chunk_count}")
                        break
                    import json
                    try:
                        data = json.loads(raw)
                    except Exception as ex:
                        _logger.warning(f"_stream_generate_online json parse failed: {ex}, raw={raw[:200]}")
                        continue
                    choices = data.get("choices", [])
                    if not choices:
                        # 调试: 记录没有 choices 的事件 (例如 usage / error 事件)
                        _logger.info(f"_stream_generate_online no-choices event: {str(data)[:200]}")
                        continue
                    delta = choices[0].get("delta", {})
                    chunk = delta.get("content", "")
                    if chunk:
                        _chunk_count += 1
                        yield chunk
                    # 检测流式结束 (finish_reason 不为 null/None 时)
                    finish_reason = choices[0].get("finish_reason")
                    if finish_reason and finish_reason != "null":
                        _logger.info(f"_stream_generate_online finish_reason={finish_reason}, total_lines={_line_count}, chunks_yielded={_chunk_count}, first_raw={_first_raw}")
