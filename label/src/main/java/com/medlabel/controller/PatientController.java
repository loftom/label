package com.medlabel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medlabel.dto.PatientCreateRequest;
import com.medlabel.entity.Patient;
import com.medlabel.mapper.PatientMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientMapper patientMapper;

    public PatientController(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    @PostMapping
    public Patient create(@Valid @RequestBody PatientCreateRequest request) {
        Patient patient = new Patient();
        patient.setPatientNo(request.getPatientNo());
        patient.setName(request.getName());
        patient.setGender(request.getGender() == null ? "U" : request.getGender());
        patient.setDob(request.getDob());
        patient.setContact(request.getContact());
        patient.setCreatedAt(LocalDateTime.now());
        patientMapper.insert(patient);
        return patient;
    }

    @GetMapping
    public List<Patient> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Patient::getPatientNo, keyword)
                    .or()
                    .like(Patient::getName, keyword);
        }
        wrapper.orderByDesc(Patient::getCreatedAt);
        return patientMapper.selectList(wrapper);
    }
}
