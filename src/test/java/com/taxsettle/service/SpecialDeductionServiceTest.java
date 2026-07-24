package com.taxsettle.service;

import com.taxsettle.entity.FamilyMember;
import com.taxsettle.entity.SpecialDeduction;
import com.taxsettle.entity.Taxpayer;
import com.taxsettle.entity.enums.DeductionType;
import com.taxsettle.repository.FamilyMemberRepository;
import com.taxsettle.repository.SpecialDeductionRepository;
import com.taxsettle.repository.TaxpayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialDeductionServiceTest {

    @Mock
    private SpecialDeductionRepository specialDeductionRepository;

    @Mock
    private TaxpayerRepository taxpayerRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @InjectMocks
    private SpecialDeductionService service;

    private Taxpayer unlockedTaxpayer;
    private Taxpayer lockedTaxpayer;

    @BeforeEach
    void setUp() {
        unlockedTaxpayer = Taxpayer.builder()
                .id(1L)
                .name("测试纳税人")
                .idNumber("110101199001010001")
                .taxYear(2025)
                .locked(false)
                .build();

        lockedTaxpayer = Taxpayer.builder()
                .id(2L)
                .name("锁定纳税人")
                .idNumber("110101199001010002")
                .taxYear(2025)
                .locked(true)
                .build();
    }

    private SpecialDeduction buildDeduction(DeductionType type, BigDecimal amount) {
        return SpecialDeduction.builder()
                .deductionType(type)
                .annualAmount(amount)
                .build();
    }

    // ---------- 单条新增 ----------

    @Nested
    @DisplayName("单条新增 create")
    class CreateTests {

        @Test
        @DisplayName("同一种扣除类型已存在时拒绝新增，不调用 save")
        void rejectsDuplicateType() {
            SpecialDeduction incoming = buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("12000"));

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(1L, DeductionType.CHILDREN_EDUCATION))
                    .thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.create(1L, incoming));
            assertTrue(ex.getMessage().contains("CHILDREN_EDUCATION"));

            verify(specialDeductionRepository, never()).save(any());
        }

        @Test
        @DisplayName("新类型新增成功")
        void acceptsNewType() {
            SpecialDeduction incoming = buildDeduction(DeductionType.HOUSING_LOAN_INTEREST, new BigDecimal("12000"));

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(1L, DeductionType.HOUSING_LOAN_INTEREST))
                    .thenReturn(false);
            when(specialDeductionRepository.save(any(SpecialDeduction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SpecialDeduction saved = service.create(1L, incoming);

            assertNotNull(saved);
            assertEquals(DeductionType.HOUSING_LOAN_INTEREST, saved.getDeductionType());
            assertEquals(new BigDecimal("12000"), saved.getAnnualAmount());
            assertEquals(unlockedTaxpayer, saved.getTaxpayer());
            verify(specialDeductionRepository, times(1)).save(any(SpecialDeduction.class));
        }

        @Test
        @DisplayName("请求体里的 taxpayer 归属被 URL 覆盖，无法越权改归属")
        void bodyTaxpayerIsOverriddenByUrl() {
            Taxpayer otherTaxpayer = Taxpayer.builder().id(99L).name("其他人").build();
            SpecialDeduction incoming = buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("24000"));
            incoming.setTaxpayer(otherTaxpayer);
            incoming.setId(123L);

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(1L, DeductionType.CHILDREN_EDUCATION))
                    .thenReturn(false);
            when(specialDeductionRepository.save(any(SpecialDeduction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SpecialDeduction saved = service.create(1L, incoming);

            assertEquals(unlockedTaxpayer.getId(), saved.getTaxpayer().getId());
            assertNull(saved.getId());
        }

        @Test
        @DisplayName("请求体不携带 taxpayer 也能正常新增（500 不再出现）")
        void worksWithoutTaxpayerInBody() {
            SpecialDeduction incoming = buildDeduction(DeductionType.HOUSING_LOAN_INTEREST, new BigDecimal("12000"));

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(1L, DeductionType.HOUSING_LOAN_INTEREST))
                    .thenReturn(false);
            when(specialDeductionRepository.save(any(SpecialDeduction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.create(1L, incoming));
            assertEquals(unlockedTaxpayer, incoming.getTaxpayer());
        }
    }

    // ---------- 批量新增 ----------

    @Nested
    @DisplayName("批量新增 createBatch")
    class CreateBatchTests {

        @Test
        @DisplayName("批次内部出现重复类型时整批拒绝，不写入任何记录")
        void rejectsIntraBatchDuplicate() {
            List<SpecialDeduction> batch = new ArrayList<>();
            batch.add(buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("24000")));
            batch.add(buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("12000")));

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.createBatch(1L, batch));
            assertTrue(ex.getMessage().contains("more than once in this batch"));

            verify(specialDeductionRepository, never()).saveAll(any());
            verify(specialDeductionRepository, never()).save(any());
        }

        @Test
        @DisplayName("批次中任意一条与已有记录类型冲突时整批拒绝")
        void rejectsWhenOneConflictsWithExisting() {
            List<SpecialDeduction> batch = new ArrayList<>();
            batch.add(buildDeduction(DeductionType.ELDERLY_SUPPORT, new BigDecimal("36000")));
            batch.add(buildDeduction(DeductionType.CONTINUING_EDUCATION_DEGREE, new BigDecimal("4800")));

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(1L, DeductionType.ELDERLY_SUPPORT))
                    .thenReturn(false);
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(1L, DeductionType.CONTINUING_EDUCATION_DEGREE))
                    .thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.createBatch(1L, batch));
            assertTrue(ex.getMessage().contains("CONTINUING_EDUCATION_DEGREE"));

            verify(specialDeductionRepository, never()).saveAll(any());
            verify(specialDeductionRepository, never()).save(any());
        }

        @Test
        @DisplayName("批次全部合法时正常写入所有记录")
        void acceptsValidBatch() {
            List<SpecialDeduction> batch = new ArrayList<>();
            batch.add(buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("24000")));
            batch.add(buildDeduction(DeductionType.HOUSING_LOAN_INTEREST, new BigDecimal("12000")));

            when(taxpayerRepository.findById(1L)).thenReturn(Optional.of(unlockedTaxpayer));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionType(eq(1L), any(DeductionType.class)))
                    .thenReturn(false);
            when(specialDeductionRepository.saveAll(anyList()))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<SpecialDeduction> saved = service.createBatch(1L, batch);

            assertEquals(2, saved.size());
            ArgumentCaptor<List<SpecialDeduction>> captor = ArgumentCaptor.forClass(List.class);
            verify(specialDeductionRepository).saveAll(captor.capture());
            List<SpecialDeduction> captured = captor.getValue();
            captured.forEach(d -> assertEquals(unlockedTaxpayer, d.getTaxpayer()));
        }
    }

    // ---------- 修改 ----------

    @Nested
    @DisplayName("修改 update")
    class UpdateTests {

        @Test
        @DisplayName("将扣除类型改成已存在的另一类型时拒绝修改")
        void rejectsChangingToConflictingType() {
            SpecialDeduction existing = SpecialDeduction.builder()
                    .id(10L)
                    .taxpayer(unlockedTaxpayer)
                    .deductionType(DeductionType.CHILDREN_EDUCATION)
                    .annualAmount(new BigDecimal("24000"))
                    .annualLimit(new BigDecimal("24000"))
                    .build();

            SpecialDeduction update = buildDeduction(DeductionType.ELDERLY_SUPPORT, new BigDecimal("36000"));

            when(specialDeductionRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionTypeAndIdNot(
                    1L, DeductionType.ELDERLY_SUPPORT, 10L)).thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.update(10L, update));
            assertTrue(ex.getMessage().contains("ELDERLY_SUPPORT"));

            verify(specialDeductionRepository, never()).save(any());
        }

        @Test
        @DisplayName("改成不冲突的类型时成功")
        void allowsChangingToNonConflictingType() {
            SpecialDeduction existing = SpecialDeduction.builder()
                    .id(10L)
                    .taxpayer(unlockedTaxpayer)
                    .deductionType(DeductionType.CHILDREN_EDUCATION)
                    .annualAmount(new BigDecimal("24000"))
                    .annualLimit(new BigDecimal("24000"))
                    .build();

            SpecialDeduction update = buildDeduction(DeductionType.HOUSING_LOAN_INTEREST, new BigDecimal("12000"));

            when(specialDeductionRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(specialDeductionRepository.existsByTaxpayerIdAndDeductionTypeAndIdNot(
                    1L, DeductionType.HOUSING_LOAN_INTEREST, 10L)).thenReturn(false);
            when(specialDeductionRepository.save(any(SpecialDeduction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SpecialDeduction saved = service.update(10L, update);

            assertEquals(DeductionType.HOUSING_LOAN_INTEREST, saved.getDeductionType());
            verify(specialDeductionRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("保持类型不变时不触发冲突检查")
        void keepsSameTypeWithoutConflictCheck() {
            SpecialDeduction existing = SpecialDeduction.builder()
                    .id(10L)
                    .taxpayer(unlockedTaxpayer)
                    .deductionType(DeductionType.CHILDREN_EDUCATION)
                    .annualAmount(new BigDecimal("24000"))
                    .annualLimit(new BigDecimal("24000"))
                    .build();

            SpecialDeduction update = buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("20000"));

            when(specialDeductionRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(specialDeductionRepository.save(any(SpecialDeduction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.update(10L, update);

            verify(specialDeductionRepository, never())
                    .existsByTaxpayerIdAndDeductionTypeAndIdNot(anyLong(), any(), anyLong());
            verify(specialDeductionRepository, times(1)).save(existing);
        }
    }

    // ---------- 锁定状态 ----------

    @Nested
    @DisplayName("锁定状态 locked")
    class LockedTests {

        @Test
        @DisplayName("单条新增：锁定纳税人时拒绝")
        void createRejectsLockedTaxpayer() {
            SpecialDeduction incoming = buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("24000"));

            when(taxpayerRepository.findById(2L)).thenReturn(Optional.of(lockedTaxpayer));

            assertThrows(IllegalStateException.class, () -> service.create(2L, incoming));
            verify(specialDeductionRepository, never()).save(any());
        }

        @Test
        @DisplayName("批量新增：锁定纳税人时整批拒绝")
        void batchRejectsLockedTaxpayer() {
            List<SpecialDeduction> batch = List.of(
                    buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("24000")));

            when(taxpayerRepository.findById(2L)).thenReturn(Optional.of(lockedTaxpayer));

            assertThrows(IllegalStateException.class, () -> service.createBatch(2L, batch));
            verify(specialDeductionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("修改：锁定纳税人时拒绝")
        void updateRejectsLockedTaxpayer() {
            SpecialDeduction existing = SpecialDeduction.builder()
                    .id(20L)
                    .taxpayer(lockedTaxpayer)
                    .deductionType(DeductionType.CHILDREN_EDUCATION)
                    .annualAmount(new BigDecimal("24000"))
                    .annualLimit(new BigDecimal("24000"))
                    .build();

            SpecialDeduction update = buildDeduction(DeductionType.CHILDREN_EDUCATION, new BigDecimal("12000"));

            when(specialDeductionRepository.findById(20L)).thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class, () -> service.update(20L, update));
            verify(specialDeductionRepository, never()).save(any());
        }

        @Test
        @DisplayName("删除：锁定纳税人时拒绝")
        void deleteRejectsLockedTaxpayer() {
            SpecialDeduction existing = SpecialDeduction.builder()
                    .id(30L)
                    .taxpayer(lockedTaxpayer)
                    .deductionType(DeductionType.CHILDREN_EDUCATION)
                    .build();

            when(specialDeductionRepository.findById(30L)).thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class, () -> service.delete(30L));
            verify(specialDeductionRepository, never()).deleteById(anyLong());
        }
    }
}
