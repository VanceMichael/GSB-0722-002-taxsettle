package com.taxsettle.config;

import com.taxsettle.entity.*;
import com.taxsettle.entity.enums.DeductionType;
import com.taxsettle.entity.enums.IncomeType;
import com.taxsettle.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final EmployerRepository employerRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final SpecialDeductionRepository specialDeductionRepository;
    private final FamilyMemberRepository familyMemberRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (taxpayerRepository.count() > 0) {
            return;
        }

        Employer companyA = employerRepository.save(Employer.builder()
                .name("星辰科技有限公司")
                .taxId("91110000MA01A1B2C3")
                .address("北京市海淀区中关村大街1号")
                .contactPerson("李会计")
                .contactPhone("010-88880001")
                .build());

        Employer companyB = employerRepository.save(Employer.builder()
                .name("云端数据集团")
                .taxId("91310000MA1F3D4E5G")
                .address("上海市浦东新区张江高科技园区")
                .contactPerson("王财务")
                .contactPhone("021-66660002")
                .build());

        Employer companyC = employerRepository.save(Employer.builder()
                .name("智慧咨询服务有限公司")
                .taxId("91440000MA2K9L8M7N")
                .address("深圳市南山区科技园")
                .contactPerson("张财务")
                .contactPhone("0755-22220003")
                .build());

        Taxpayer tp1 = seedTaxpayer1(companyA, companyC);
        Taxpayer tp2 = seedTaxpayer2(companyA);
        Taxpayer tp3 = seedTaxpayer3(companyB);
        Taxpayer tp4 = seedTaxpayer4(companyA, tp1);

        seedFamilyMembers(tp1, tp2, tp4);
    }

    private void seedFamilyMembers(Taxpayer tp1, Taxpayer tp2, Taxpayer tp4) {
        FamilyMember child1 = familyMemberRepository.save(FamilyMember.builder()
                .taxpayer(tp1)
                .name("张小宝")
                .idNumber("110101201501011234")
                .relationship("子女")
                .remark("张明远的儿子")
                .build());

        familyMemberRepository.save(FamilyMember.builder()
                .taxpayer(tp1)
                .name("张父")
                .idNumber("110101195001019876")
                .relationship("父亲")
                .remark("张明远的父亲")
                .build());

        familyMemberRepository.save(FamilyMember.builder()
                .taxpayer(tp2)
                .name("张小宝")
                .idNumber("110101201501011234")
                .relationship("子女")
                .remark("刘思雨的儿子，与张明远共同抚养")
                .build());

        familyMemberRepository.save(FamilyMember.builder()
                .taxpayer(tp4)
                .name("张小宝")
                .idNumber("110101201501011234")
                .relationship("子女")
                .remark("重复申报测试")
                .build());

        List<SpecialDeduction> deductions = specialDeductionRepository.findByTaxpayerId(tp1.getId());
        for (SpecialDeduction d : deductions) {
            if (d.getDeductionType() == DeductionType.CHILDREN_EDUCATION) {
                d.setFamilyMember(child1);
                specialDeductionRepository.save(d);
            }
        }
    }

    private Taxpayer seedTaxpayer1(Employer employer, Employer employer2) {
        Taxpayer tp = taxpayerRepository.save(Taxpayer.builder()
                .name("张明远")
                .idNumber("110101199001011234")
                .employer(employer)
                .taxYear(2025)
                .socialInsuranceDeduction(new BigDecimal("24000"))
                .build());

        List<IncomeRecord> incomes = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.WAGES)
                    .month(m)
                    .grossIncome(new BigDecimal("25000"))
                    .prepaidTax(new BigDecimal("1140"))
                    .employer(employer)
                    .build());
        }
        for (int m = 1; m <= 6; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.LABOR_REMUNERATION)
                    .month(m)
                    .grossIncome(new BigDecimal("5000"))
                    .prepaidTax(new BigDecimal("800"))
                    .employer(employer2)
                    .build());
        }
        incomeRecordRepository.saveAll(incomes);

        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .annualLimit(new BigDecimal("24000"))
                .remark("1个子女，每月2000元")
                .build());
        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("36000"))
                .annualLimit(new BigDecimal("36000"))
                .remark("独生子女，每月3000元")
                .build());
        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.HOUSING_LOAN_INTEREST)
                .annualAmount(new BigDecimal("12000"))
                .annualLimit(new BigDecimal("12000"))
                .remark("首套住房贷款利息")
                .build());
        return tp;
    }

    private Taxpayer seedTaxpayer2(Employer employer) {
        Taxpayer tp = taxpayerRepository.save(Taxpayer.builder()
                .name("刘思雨")
                .idNumber("310101199505052345")
                .employer(employer)
                .taxYear(2025)
                .socialInsuranceDeduction(new BigDecimal("18000"))
                .build());

        List<IncomeRecord> incomes = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal wage = m == 6 ? new BigDecimal("35000") : new BigDecimal("12000");
            BigDecimal tax = m == 6 ? new BigDecimal("3840") : new BigDecimal("360");
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.WAGES)
                    .month(m)
                    .grossIncome(wage)
                    .prepaidTax(tax)
                    .employer(employer)
                    .build());
        }
        for (int m = 3; m <= 5; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.AUTHOR_REMUNERATION)
                    .month(m)
                    .grossIncome(new BigDecimal("8000"))
                    .prepaidTax(new BigDecimal("896"))
                    .build());
        }
        incomeRecordRepository.saveAll(incomes);

        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.CONTINUING_EDUCATION_DEGREE)
                .annualAmount(new BigDecimal("4800"))
                .annualLimit(new BigDecimal("4800"))
                .remark("学历(学位)继续教育")
                .build());
        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("18000"))
                .annualLimit(new BigDecimal("18000"))
                .remark("非独生子女，每月1500元")
                .build());
        return tp;
    }

    private Taxpayer seedTaxpayer3(Employer employer) {
        Taxpayer tp = taxpayerRepository.save(Taxpayer.builder()
                .name("陈志豪")
                .idNumber("440101198812123456")
                .employer(employer)
                .taxYear(2025)
                .socialInsuranceDeduction(new BigDecimal("42000"))
                .build());

        List<IncomeRecord> incomes = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.WAGES)
                    .month(m)
                    .grossIncome(new BigDecimal("45000"))
                    .prepaidTax(new BigDecimal("600"))
                    .employer(employer)
                    .build());
        }
        for (int m = 1; m <= 4; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.ROYALTIES)
                    .month(m)
                    .grossIncome(new BigDecimal("20000"))
                    .prepaidTax(BigDecimal.ZERO)
                    .build());
        }
        for (int m = 6; m <= 10; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.LABOR_REMUNERATION)
                    .month(m)
                    .grossIncome(new BigDecimal("15000"))
                    .prepaidTax(BigDecimal.ZERO)
                    .build());
        }
        incomeRecordRepository.saveAll(incomes);

        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.CHILDREN_EDUCATION)
                .annualAmount(new BigDecimal("24000"))
                .annualLimit(new BigDecimal("24000"))
                .remark("1个子女")
                .build());
        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.ELDERLY_SUPPORT)
                .annualAmount(new BigDecimal("36000"))
                .annualLimit(new BigDecimal("36000"))
                .remark("独生子女")
                .build());
        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.HOUSING_LOAN_INTEREST)
                .annualAmount(new BigDecimal("12000"))
                .annualLimit(new BigDecimal("12000"))
                .remark("首套住房贷款利息")
                .build());
        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.CONTINUING_EDUCATION_CERTIFICATION)
                .annualAmount(new BigDecimal("3600"))
                .annualLimit(new BigDecimal("3600"))
                .remark("职业资格取证")
                .build());
        return tp;
    }

    private Taxpayer seedTaxpayer4(Employer employer, Taxpayer referenceTp) {
        Taxpayer tp = taxpayerRepository.save(Taxpayer.builder()
                .name("赵晓雯")
                .idNumber("320101199208084567")
                .employer(employer)
                .taxYear(2025)
                .socialInsuranceDeduction(new BigDecimal("30000"))
                .build());

        List<IncomeRecord> incomes = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            incomes.add(IncomeRecord.builder()
                    .taxpayer(tp)
                    .incomeType(IncomeType.WAGES)
                    .month(m)
                    .grossIncome(new BigDecimal("30000"))
                    .prepaidTax(new BigDecimal("1800"))
                    .employer(employer)
                    .build());
        }
        incomeRecordRepository.saveAll(incomes);

        specialDeductionRepository.save(SpecialDeduction.builder()
                .taxpayer(tp)
                .deductionType(DeductionType.HOUSING_LOAN_INTEREST)
                .annualAmount(new BigDecimal("12000"))
                .annualLimit(new BigDecimal("12000"))
                .remark("首套住房贷款利息")
                .build());
        return tp;
    }
}
