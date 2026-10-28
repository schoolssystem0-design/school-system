package com.selectcode.sms.iam.domain;

import static com.selectcode.sms.shared.security.Permissions.ACADEMICS_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.ACADEMICS_VIEW;
import static com.selectcode.sms.shared.security.Permissions.AUDIT_VIEW;
import static com.selectcode.sms.shared.security.Permissions.IAM_LOGIN_HISTORY_VIEW;
import static com.selectcode.sms.shared.security.Permissions.IAM_ROLE_VIEW;
import static com.selectcode.sms.shared.security.Permissions.IAM_USER_VIEW;
import static com.selectcode.sms.shared.security.Permissions.SCHOOL_CALENDAR_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.SCHOOL_PROFILE_EDIT;
import static com.selectcode.sms.shared.security.Permissions.SCHOOL_PROFILE_VIEW;
import static com.selectcode.sms.shared.security.Permissions.SCHOOL_YEAR_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.SETTINGS_EDIT;
import static com.selectcode.sms.shared.security.Permissions.SETTINGS_VIEW;

import static com.selectcode.sms.shared.security.Permissions.ADMISSIONS_ENROL;
import static com.selectcode.sms.shared.security.Permissions.ADMISSIONS_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.ADMISSIONS_VIEW;
import static com.selectcode.sms.shared.security.Permissions.FAMILIES_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.FAMILIES_VIEW;
import static com.selectcode.sms.shared.security.Permissions.STAFF_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.STAFF_SALARY_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.STAFF_SALARY_VIEW;
import static com.selectcode.sms.shared.security.Permissions.STAFF_VIEW;
import static com.selectcode.sms.shared.security.Permissions.STUDENTS_MANAGE;
import static com.selectcode.sms.shared.security.Permissions.STUDENTS_MOVE;
import static com.selectcode.sms.shared.security.Permissions.STUDENTS_PRINT;
import static com.selectcode.sms.shared.security.Permissions.STUDENTS_PRIVATE_VIEW;
import static com.selectcode.sms.shared.security.Permissions.STUDENTS_VIEW;

import com.selectcode.sms.shared.security.Permissions;
import java.util.List;
import java.util.Set;

/**
 * The roles every school starts with (FR 2 account types, DECISIONS D-13). Schools may edit them (except
 * Administrator) and create their own. When a later phase adds permissions, extend these templates: existing schools'
 * system roles receive the newly added permissions on startup ({@code SystemRoleUpgrader}).
 */
public final class RoleTemplates {

	public static final String ADMIN = "ADMIN";

	public record Template(String code, String name, String description, Set<String> permissions) {
	}

	private static final Set<String> VIEW_ALL = Set.of(SCHOOL_PROFILE_VIEW, IAM_USER_VIEW, IAM_ROLE_VIEW,
			IAM_LOGIN_HISTORY_VIEW, AUDIT_VIEW, SETTINGS_VIEW, ACADEMICS_VIEW, ADMISSIONS_VIEW, STUDENTS_VIEW,
			STUDENTS_PRIVATE_VIEW, FAMILIES_VIEW, STAFF_VIEW, STAFF_SALARY_VIEW);

	public static final List<Template> ALL = List.of(
			new Template(ADMIN, "Administrator", "Full set-up, users, settings and all modules", Permissions.CODES),
			new Template("PRINCIPAL", "Principal", "All academic and administrative information, approvals, reports",
					union(VIEW_ALL, Set.of(SCHOOL_PROFILE_EDIT, SCHOOL_YEAR_MANAGE, SCHOOL_CALENDAR_MANAGE,
							SETTINGS_EDIT, ACADEMICS_MANAGE, ADMISSIONS_MANAGE, ADMISSIONS_ENROL, STUDENTS_MANAGE,
							STUDENTS_MOVE, STUDENTS_PRINT, FAMILIES_MANAGE, STAFF_MANAGE))),
			new Template("SCHOOL_OWNER", "School owner", "Read-only view of finance, reports and the dashboard",
					VIEW_ALL),
			new Template("AUDITOR", "Auditor", "Read-only finance and activity history",
					Set.of(AUDIT_VIEW, IAM_LOGIN_HISTORY_VIEW, SCHOOL_PROFILE_VIEW, SETTINGS_VIEW, STUDENTS_VIEW,
							FAMILIES_VIEW, STAFF_VIEW, STAFF_SALARY_VIEW)),
			new Template("ACCOUNTANT", "Accounts staff", "Fees, receipts, school accounts, salary records",
					Set.of(SCHOOL_PROFILE_VIEW, SETTINGS_VIEW, ACADEMICS_VIEW, STUDENTS_VIEW, STUDENTS_PRIVATE_VIEW,
							FAMILIES_VIEW, STAFF_VIEW, STAFF_SALARY_VIEW, STAFF_SALARY_MANAGE)),
			new Template("TEACHER", "Teacher", "Own classes: attendance, marks, diary, timetable",
					Set.of(ACADEMICS_VIEW, STUDENTS_VIEW)),
			new Template("FRONT_OFFICE", "Front office", "Enquiries, visitors, basic student look-up",
					Set.of(SCHOOL_PROFILE_VIEW, ACADEMICS_VIEW, ADMISSIONS_VIEW, ADMISSIONS_MANAGE, ADMISSIONS_ENROL,
							STUDENTS_VIEW, STUDENTS_PRIVATE_VIEW, STUDENTS_MANAGE, STUDENTS_PRINT, FAMILIES_VIEW,
							FAMILIES_MANAGE)),
			new Template("EXAM_COORDINATOR", "Exam coordinator", "Exams, marks, results and report cards",
					Set.of(ACADEMICS_VIEW, ACADEMICS_MANAGE, SETTINGS_VIEW, STUDENTS_VIEW, STAFF_VIEW)),
			new Template("WEBSITE_EDITOR", "Website editor", "School website content and online forms",
					Set.of(SCHOOL_PROFILE_VIEW)));

	private RoleTemplates() {
	}

	private static Set<String> union(Set<String> a, Set<String> b) {
		var all = new java.util.HashSet<>(a);
		all.addAll(b);
		return Set.copyOf(all);
	}
}
