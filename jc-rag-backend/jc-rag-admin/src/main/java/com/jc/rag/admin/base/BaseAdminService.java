package com.jc.rag.admin.base;

import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * admin 濞戞挻鑹炬慨?Service 闁硅泛鈧喕鏉介柛鈺勬鐞氼偊濡? * <p>闁告瑥鍊介埀?MyBatis-Plus 濡炲瀛╅悧鎼佹晬鐏炴儳绲瑰〒?6 濠㈠爢鍐敤缁绢厸鍋?CRUD 闁瑰灝绉崇紞鏃堟晬濮濈浛ge / getById / list / save / update / removeById闁? * <p>閻庢稒鍔楃悮顐﹀矗椤忓牊浠橀柟绋挎搐閻?DTO 缂侇偉顕ч悗閿嬬▔?VO 缂侇偉顕ч悗鐑芥晬鐏炵偓锟ラ梻鍥ｅ亾缂傚倹鐗曢崯鎾诲冀闁垮绶查柛鎺戞閵?閺夌儐鍓氬畷鍙夌閿濆洨鍨抽柕? * <p>@author JC
 */
public abstract class BaseAdminService<D, V, ID> {

    /**
     * 闁告梻濮惧ù鍥础閺囶潿鈧?DTO 闁告帗顨夐妴鍐Υ?     * <p>闁汇垹宕悺娆戠尵缂佹ɑ鏆堥柛?mapper 闁瑰瓨鐗為崵婊呪偓瑙勭煯缁?SQL闁?     */
    protected abstract List<D> loadPage(PageRequest request);

    /**
     * 缂備胶鍠曢鎼佸箑缂佹ɑ娈堕柕?     */
    protected abstract long countPage(PageRequest request);

    /**
     * DTO 闁?VO 閺夌儐鍓氬畷鏌ュΥ?     */
    protected abstract V toVO(D dto);

    /**
     * 闁告梻濮惧ù鍥礂閵娾晛鍔?DTO 闁告帗顨夐妴鍐Υ?     */
    protected abstract List<D> loadAll();

    /**
     * 闁?ID 闁告梻濮惧ù?DTO闁?     */
    protected abstract D loadById(ID id);

    /**
     * 闁告帗绋戠紓鎾诲Υ?     * <p>閺夆晜鏌ㄥú鏍暜閿斿灝鐦滈梺娆惧枤濞?DTO闁?     */
    protected abstract D doCreate(D dto);

    /**
     * 闁哄洤鐡ㄩ弻濠囧Υ?     */
    protected abstract void doUpdate(ID id, D dto);

    /**
     * 闁告帞濞€濞呭酣濡?     */
    protected abstract void doDelete(ID id);

    // ========== 闂侇偅姘ㄩ弫銈夊棘鐟欏嫮銆?==========

    /** 闁告帒妫濋妴澶愬蓟閵夘煈鍤勯柨娑欘儚TO 闁?VO闁?*/
    public PageResponse<V> page(PageRequest request) {
        long total = countPage(request);
        if (total == 0) {
            return PageResponse.of(List.of(), 0, request.getPageNum(), request.getPageSize());
        }
        List<D> records = loadPage(request);
        return PageResponse.of(toVOList(records), total, request.getPageNum(), request.getPageSize());
    }

    /** 闁稿繈鍔戦崕鎾礆濡ゅ嫨鈧啴濡?*/
    public List<V> list() {
        return toVOList(loadAll());
    }

    /** 闁?ID 闁哄被鍎撮妤呭Υ?*/
    public V getById(ID id) {
        D dto = loadById(id);
        return dto == null ? null : toVO(dto);
    }

    /** 闁哄倹婢橀·鍐Υ?*/
    public V create(D dto) {
        D saved = doCreate(dto);
        return saved == null ? null : toVO(saved);
    }

    /** 闁哄洤鐡ㄩ弻濠囧Υ?*/
    public V update(ID id, D dto) {
        doUpdate(id, dto);
        return getById(id);
    }

    /** 闁告帞濞€濞呭酣濡?*/
    public void delete(ID id) {
        doDelete(id);
    }

    // ========== 鐎规悶鍎遍崣鍧楀棘鐟欏嫮銆?==========

    /** List<DTO> 闁?List<VO>闁?*/
    protected List<V> toVOList(List<D> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(this::toVO).toList();
    }
}
