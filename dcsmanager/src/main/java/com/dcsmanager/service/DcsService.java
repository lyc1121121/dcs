package com.dcsmanager.service;

import com.dcsmanager.domain.Dcs;
import com.dcsmanager.domain.DcsIdUtils;
import com.dcsmanager.repository.DcsRepository;
import com.dcsmanager.web.DcsForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DcsService {

    private final DcsRepository dcsRepository;

    public DcsService(DcsRepository dcsRepository) {
        this.dcsRepository = dcsRepository;
    }

    @Transactional(readOnly = true)
    public List<Dcs> findAll() {
        return dcsRepository.findAllByOrderByDcsIdAsc();
    }

    @Transactional(readOnly = true)
    public Dcs get(String dcsId) {
        return dcsRepository.findById(dcsId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 TMS_ID 입니다: " + dcsId));
    }

    public Dcs create(DcsForm form) {
        if (dcsRepository.existsById(form.getDcsId())) {
            throw new IllegalArgumentException("이미 존재하는 TMS_ID 입니다: " + form.getDcsId());
        }

        Dcs dcs = new Dcs();
        dcs.setDcsId(form.getDcsId());
        applyForm(dcs, form);
        return dcsRepository.save(dcs);
    }

    public Dcs update(String dcsId, DcsForm form) {
        Dcs dcs = get(dcsId);
        applyForm(dcs, form);
        return dcsRepository.save(dcs);
    }

    public void delete(String dcsId) {
        Dcs dcs = get(dcsId);
        dcsRepository.delete(dcs);
    }

    private void applyForm(Dcs dcs, DcsForm form) {
        dcs.setDcsImageVersion(form.getDcsImageVersion());
        dcs.setDcsDotId(DcsIdUtils.toDotId(dcs.getDcsId()));
        dcs.setDcsMode(form.getDcsMode());
        dcs.setDcsSize(form.getDcsSize());
        dcs.setPortDcs1(form.getPortDcs1());
        dcs.setPortDcs2(form.getPortDcs2());
        dcs.setDcsServerIp(form.getDcsServerIp());
    }
}
