package com.taxsettle.service;

import com.taxsettle.entity.SpecialDeduction;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.DeductionType;
import com.taxsettle.repository.SpecialDeductionRepository;
import com.taxsettle.repository.TaxpayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SpecialDeductionServiceTest {

    @Autowired
    private SpecialDeductionService specialDeductionService;

    @Autowired
    private SpecialDeductionRepository specialDeductionRepository;

    @Autowired
    private TaxpayerRepository taxpayerRepository;

    private Taxpayer unlockedTaxpayer;
    private Taxpayer lockedTaxpayer;

    @BeforeEach
    void setUp() {
        specialDeductionRepository.deleteAll();
        taxpayerRepository.deleteAll();

        unlockedTaxpayer = taxpayerRepository.save(Taxpayer.builder()
                .name("测试纳税人A")
                .idNumber("TEST000000000001")
                .taxYear(2025)
                .locked(false)
                .build());

        lockedTaxpayer = taxpayerRepository.save(Taxpayer.builder()
                .name("测试纳税人B(已锁定)")
                .idNumber("TEST000000000002")
                .taxYear(2025)
                .locked(true)
                .build());
    }

    @Test
    @DisplayName("单条新增: 不同扣除类型可以成功创建")
    void create_singleDifferentTypes_success() {
        SpecialDeduction d1 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        SpecialDeduction saved1 = specialDeductionService.create(d1);
        assertNotNull(saved1.getId());
        assertEquals(DeductionType.CHILDREN_EDUCATION, saved1.getDeductionType());

        SpecialDeduction d2 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("36000"))
                .build();
        SpecialDeduction saved2 = specialDeductionService.create(d2);
        assertNotNull(saved2.getId());
        assertEquals(2, specialDeductionRepository.findByTaxpayerId(unlockedTaxpayer.getId()).size());
    }

    @Test
    @DisplayName("单条新增: 同类型扣除重复录入应抛出异常")
    void create_duplicateType_throwsException() {
        SpecialDeduction d1 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        specialDeductionService.create(d1);

        SpecialDeduction d2 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> specialDeductionService.create(d2));
        assertTrue(ex.getMessage().contains("Duplicate deduction type"));
        assertEquals(1, specialDeductionRepository.findByTaxpayerId(unlockedTaxpayer.getId()).size());
    }

    @Test
    @DisplayName("批量新增: 不同类型整批成功写入")
    void createBatch_allUniqueTypes_success() {
        List<SpecialDeduction> batch = new ArrayList<>();
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build());
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.HOUSING_LOAN_INTEREST)
                .annualAmount(new BigDecimal("12000"))
                .build());
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.CONTINUING_EDUCATION_DEGREE)
                .annualAmount(new BigDecimal("4800"))
                .build());

        List<SpecialDeduction> saved = specialDeductionService.createBatch(unlockedTaxpayer.getId(), batch);
        assertEquals(3, saved.size());
        assertEquals(3, specialDeductionRepository.findByTaxpayerId(unlockedTaxpayer.getId()).size());
    }

    @Test
    @DisplayName("批量新增: 批次内包含重复类型，整批不能写入")
    void createBatch_duplicateWithinBatch_allRejected() {
        List<SpecialDeduction> batch = new ArrayList<>();
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build());
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("12000"))
                .build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> specialDeductionService.createBatch(unlockedTaxpayer.getId(), batch));
        assertTrue(ex.getMessage().contains("Duplicate deduction type within batch"));
        assertEquals(0, specialDeductionRepository.findByTaxpayerId(unlockedTaxpayer.getId()).size());
    }

    @Test
    @DisplayName("批量新增: 批次内某条与已有记录冲突，整批不能写入")
    void createBatch_conflictWithExisting_allRejected() {
        SpecialDeduction existing = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        specialDeductionService.create(existing);

        List<SpecialDeduction> batch = new ArrayList<>();
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("36000"))
                .build());
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build());
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.HOUSING_LOAN_INTEREST)
                .annualAmount(new BigDecimal("12000"))
                .build());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> specialDeductionService.createBatch(unlockedTaxpayer.getId(), batch));
        assertTrue(ex.getMessage().contains("Duplicate deduction type"));
        assertEquals(1, specialDeductionRepository.findByTaxpayerId(unlockedTaxpayer.getId()).size());
    }

    @Test
    @DisplayName("修改: 修改金额/备注不产生冲突可以成功")
    void update_changeAmountNoConflict_success() {
        SpecialDeduction d = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .remark("原始")
                .build();
        SpecialDeduction saved = specialDeductionService.create(d);

        SpecialDeduction update = SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("20000"))
                .remark("修改后")
                .build();
        SpecialDeduction result = specialDeductionService.update(saved.getId(), update);
        assertEquals(new BigDecimal("20000"), result.getAnnualAmount());
        assertEquals("修改后", result.getRemark());
    }

    @Test
    @DisplayName("修改: 将扣除类型改成已有冲突类型应失败")
    void update_changeTypeToConflict_throwsException() {
        SpecialDeduction d1 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        SpecialDeduction saved1 = specialDeductionService.create(d1);

        SpecialDeduction d2 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("36000"))
                .build();
        specialDeductionService.create(d2);

        SpecialDeduction update = SpecialDeduction.builder()
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("36000"))
                .build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> specialDeductionService.update(saved1.getId(), update));
        assertTrue(ex.getMessage().contains("Duplicate deduction type after update"));

        SpecialDeduction unchanged = specialDeductionRepository.findById(saved1.getId()).orElseThrow();
        assertEquals(DeductionType.CHILDREN_EDUCATION, unchanged.getDeductionType());
    }

    @Test
    @DisplayName("修改: 将类型改成不冲突的新类型可以成功")
    void update_changeTypeToNewType_success() {
        SpecialDeduction d = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        SpecialDeduction saved = specialDeductionService.create(d);

        SpecialDeduction update = SpecialDeduction.builder()
                .deductionType(DeductionType.CONTINUING_EDUCATION_CERTIFICATION)
                .annualAmount(new BigDecimal("3600"))
                .build();
        SpecialDeduction result = specialDeductionService.update(saved.getId(), update);
        assertEquals(DeductionType.CONTINUING_EDUCATION_CERTIFICATION, result.getDeductionType());
    }

    @Test
    @DisplayName("锁定状态: 已锁定纳税人不能新增扣除")
    void lockedTaxpayer_create_throwsException() {
        SpecialDeduction d = SpecialDeduction.builder()
                .taxpayer(lockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> specialDeductionService.create(d));
        assertTrue(ex.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("锁定状态: 已锁定纳税人不能批量新增扣除")
    void lockedTaxpayer_createBatch_throwsException() {
        List<SpecialDeduction> batch = new ArrayList<>();
        batch.add(SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .build());

        assertThrows(IllegalStateException.class,
                () -> specialDeductionService.createBatch(lockedTaxpayer.getId(), batch));
    }

    @Test
    @DisplayName("锁定状态: 已锁定纳税人已有扣除不能修改")
    void lockedTaxpayer_update_throwsException() {
        SpecialDeduction existing = specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(lockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .annualLimit(new BigDecimal("24000"))
                .build());

        SpecialDeduction update = SpecialDeduction.builder()
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("12000"))
                .build();
        assertThrows(IllegalStateException.class,
                () -> specialDeductionService.update(existing.getId(), update));
    }

    @Test
    @DisplayName("锁定状态: 已锁定纳税人已有扣除不能删除")
    void lockedTaxpayer_delete_throwsException() {
        SpecialDeduction existing = specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(lockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .annualLimit(new BigDecimal("24000"))
                .build());

        assertThrows(IllegalStateException.class,
                () -> specialDeductionService.delete(existing.getId()));
    }

    @Test
    @DisplayName("数据库唯一约束: 同一纳税人同一类型在数据库层面也无法重复插入")
    void dbUniqueConstraint_preventsDuplicate() {
        SpecialDeduction d1 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .annualLimit(new BigDecimal("24000"))
                .build();
        specialDeductionRepository.save(d1);

        SpecialDeduction d2 = SpecialDeduction.builder()
                .taxpayer(unlockedTaxpayer)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("12000"))
                .annualLimit(new BigDecimal("24000"))
                .build();
        assertThrows(Exception.class, () -> specialDeductionRepository.saveAndFlush(d2));
    }
}
