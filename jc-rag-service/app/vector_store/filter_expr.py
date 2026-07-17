"""
过滤表达式 DSL - 后端无关, 由各 VectorStore 实现类翻译成原生语法.
业务代码只构造 FilterExpr, 不得直接构造 ES/Milvus DSL.

@author JC
@since 2026-07-16
"""
from __future__ import annotations

from typing import Any


class FilterCondition:
    """单条过滤条件"""

    __slots__ = ("field", "op", "value")

    def __init__(self, field: str, op: str, value: Any) -> None:
        self.field = field
        self.op = op
        self.value = value

    def __repr__(self) -> str:
        return f"FilterCondition({self.field} {self.op} {self.value!r})"


class OrGroup:
    """OR 分组 - 多个 FilterExpr 至少一个匹配"""

    __slots__ = ("exprs",)

    def __init__(self, exprs: list["FilterExpr"]) -> None:
        self.exprs = exprs


class FilterExpr:
    """过滤表达式 - 多个 condition / OR group 的 AND 组合"""

    __slots__ = ("items",)

    def __init__(
        self,
        items: list[FilterCondition | OrGroup] | None = None,
    ) -> None:
        self.items = items or []

    # ---- 工厂方法 ----
    @staticmethod
    def eq(field: str, value: Any) -> "FilterExpr":
        return FilterExpr([FilterCondition(field, "eq", value)])

    @staticmethod
    def in_(field: str, values: list[Any]) -> "FilterExpr":
        return FilterExpr([FilterCondition(field, "in", values)])

    @staticmethod
    def range_(
        field: str,
        gte: Any = None,
        lte: Any = None,
        gt: Any = None,
        lt: Any = None,
    ) -> "FilterExpr":
        return FilterExpr([FilterCondition(field, "range", {
            "gte": gte, "lte": lte, "gt": gt, "lt": lt,
        })])

    # ---- 组合 ----
    def and_(self, other: "FilterExpr") -> "FilterExpr":
        return FilterExpr(self.items + other.items)

    def or_(self, other: "FilterExpr") -> "FilterExpr":
        return FilterExpr(self.items + [OrGroup([self, other])])

    # ---- 后端翻译 ----
    def to_es(self) -> list[dict[str, Any]]:
        result: list[dict[str, Any]] = []
        for item in self.items:
            if isinstance(item, FilterCondition):
                result.append(_cond_to_es(item))
            elif isinstance(item, OrGroup):
                result.append(_or_to_es(item))
        return result

    def to_milvus(self) -> str:
        parts: list[str] = []
        for item in self.items:
            if isinstance(item, FilterCondition):
                parts.append(_cond_to_milvus(item))
            elif isinstance(item, OrGroup):
                parts.append(_or_to_milvus(item))
        return " and ".join(parts)


def _cond_to_es(c: FilterCondition) -> dict[str, Any]:
    if c.op == "eq":
        return {"term": {c.field: c.value}}
    if c.op == "in":
        return {"terms": {c.field: c.value}}
    if c.op == "range":
        return {"range": {c.field: c.value}}
    raise ValueError(f"Unsupported op: {c.op}")


def _or_to_es(g: OrGroup) -> dict[str, Any]:
    return {
        "bool": {
            "should": [
                e.to_es() for e in g.exprs if isinstance(e, FilterExpr)
            ],
            "minimum_should_match": 1,
        }
    }


def _format_milvus_value(v: Any) -> str:
    """按 Python 类型推断 Milvus 字面量, 避免 bool/int 误加引号导致类型比较失败.

    <p>bool → true/false (不加引号, Milvus BOOL 字段)
    <p>int/float → 原样 (不加引号, Milvus INT64/FLOAT 字段)
    <p>None → null
    <p>str → 加引号 + 转义内嵌引号 (VARCHAR 字段)
    """
    if isinstance(v, bool):  # bool 是 int 子类, 必须先判
        return "true" if v else "false"
    if isinstance(v, (int, float)):
        return str(v)
    if v is None:
        return "null"
    s = str(v).replace('"', '\\"')
    return f'"{s}"'


def _cond_to_milvus(c: FilterCondition) -> str:
    if c.op == "eq":
        return f'{c.field} == {_format_milvus_value(c.value)}'
    if c.op == "in":
        v = ", ".join(_format_milvus_value(x) for x in c.value)
        return f'{c.field} in [{v}]'
    if c.op == "range":
        r = c.value
        parts: list[str] = []
        if r.get("gte") is not None:
            parts.append(f'{c.field} >= {_format_milvus_value(r["gte"])}')
        if r.get("lte") is not None:
            parts.append(f'{c.field} <= {_format_milvus_value(r["lte"])}')
        return " and ".join(parts)
    raise ValueError(f"Unsupported op: {c.op}")


def _or_to_milvus(g: OrGroup) -> str:
    parts: list[str] = []
    for e in g.exprs:
        if isinstance(e, FilterExpr):
            parts.append(f"({e.to_milvus()})")
    return " or ".join(parts)
