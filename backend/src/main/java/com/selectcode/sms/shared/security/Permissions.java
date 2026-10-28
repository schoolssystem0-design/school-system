package com.selectcode.sms.shared.security;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The permission catalogue (FR 1.7, 2.5). Codes are {@code module.resource.action}. Constants are compile-time strings
 * so they can be used in {@code @PreAuthorize("hasAuthority('" + Permissions.X + "')")}.
 *
 * <p>When a phase adds permissions, add them here with the next {@code since} version and add them to the role
 * templates in {@code iam.domain.RoleTemplates}; existing schools' system roles are upgraded on startup.
 */
public final class Permissions {

	public static final String SCHOOL_PROFILE_VIEW = "school.profile.view";

	public static final String SCHOOL_PROFILE_EDIT = "school.profile.edit";

	public static final String SCHOOL_YEAR_MANAGE = "school.year.manage";

	public static final String SCHOOL_YEAR_CORRECT_CLOSED = "school.year.correct_closed";

	public static final String SCHOOL_CALENDAR_MANAGE = "school.calendar.manage";

	public static final String IAM_USER_VIEW = "iam.user.view";

	public static final String IAM_USER_MANAGE = "iam.user.manage";

	public static final String IAM_ROLE_VIEW = "iam.role.view";

	public static final String IAM_ROLE_MANAGE = "iam.role.manage";

	public static final String IAM_LOGIN_HISTORY_VIEW = "iam.login_history.view";

	public static final String AUDIT_VIEW = "audit.view";

	public static final String SETTINGS_VIEW = "settings.view";

	public static final String SETTINGS_EDIT = "settings.edit";

	public static final String ACADEMICS_VIEW = "academics.view";

	public static final String ACADEMICS_MANAGE = "academics.manage";

	// ---- Phase 2: admissions, students, families, staff
	public static final String ADMISSIONS_VIEW = "admissions.view";

	public static final String ADMISSIONS_MANAGE = "admissions.manage";

	public static final String ADMISSIONS_ENROL = "admissions.enrol";

	public static final String STUDENTS_VIEW = "students.view";

	public static final String STUDENTS_MANAGE = "students.manage";

	public static final String STUDENTS_MOVE = "students.move";

	public static final String STUDENTS_PRIVATE_VIEW = "students.private.view";

	public static final String STUDENTS_PRINT = "students.print";

	public static final String FAMILIES_VIEW = "families.view";

	public static final String FAMILIES_MANAGE = "families.manage";

	public static final String STAFF_VIEW = "staff.view";

	public static final String STAFF_MANAGE = "staff.manage";

	public static final String STAFF_SALARY_VIEW = "staff.salary.view";

	public static final String STAFF_SALARY_MANAGE = "staff.salary.manage";

	public record Definition(String code, String group, String description, int since) {
	}

	public static final List<Definition> ALL = List.of(
			new Definition(SCHOOL_PROFILE_VIEW, "School setup", "View school profile and document identity", 1),
			new Definition(SCHOOL_PROFILE_EDIT, "School setup", "Edit school profile, logo, stamp and signatures", 1),
			new Definition(SCHOOL_YEAR_MANAGE, "School setup", "Create, open and close academic years", 1),
			new Definition(SCHOOL_YEAR_CORRECT_CLOSED, "School setup", "Correct records in a closed academic year", 1),
			new Definition(SCHOOL_CALENDAR_MANAGE, "School setup", "Set working days and holidays", 1),
			new Definition(IAM_USER_VIEW, "Users & security", "View user accounts", 1),
			new Definition(IAM_USER_MANAGE, "Users & security",
					"Add, edit, activate/deactivate users and reset passwords", 1),
			new Definition(IAM_ROLE_VIEW, "Users & security", "View roles and their permissions", 1),
			new Definition(IAM_ROLE_MANAGE, "Users & security", "Create and change roles and permissions", 1),
			new Definition(IAM_LOGIN_HISTORY_VIEW, "Users & security", "View login history and failed attempts", 1),
			new Definition(AUDIT_VIEW, "Users & security", "View activity history (audit trail)", 1),
			new Definition(SETTINGS_VIEW, "Rules & settings", "View school rules and settings", 1),
			new Definition(SETTINGS_EDIT, "Rules & settings", "Change school rules and settings", 1),
			new Definition(ACADEMICS_VIEW, "Classes & subjects", "View classes, sections and subjects", 1),
			new Definition(ACADEMICS_MANAGE, "Classes & subjects",
					"Manage classes, sections, subjects, maximum marks, teaching assignments and timetable", 1),
			new Definition(ADMISSIONS_VIEW, "Admissions", "View admission enquiries", 2),
			new Definition(ADMISSIONS_MANAGE, "Admissions",
					"Add and follow up enquiries, statuses, entry tests, terms and syllabus", 2),
			new Definition(ADMISSIONS_ENROL, "Admissions", "Enrol students (create the student record)", 2),
			new Definition(STUDENTS_VIEW, "Students", "View students (limited to assigned classes for teachers)", 2),
			new Definition(STUDENTS_MANAGE, "Students", "Edit student details, documents, activities, bulk changes", 2),
			new Definition(STUDENTS_MOVE, "Students", "Move section/class, promote, repeat and withdraw students", 2),
			new Definition(STUDENTS_PRIVATE_VIEW, "Students",
					"See private details: phone numbers, address, CNIC/B-Form, documents", 2),
			new Definition(STUDENTS_PRINT, "Students", "Print ID cards, certificates and student records", 2),
			new Definition(FAMILIES_VIEW, "Families", "View family records and siblings", 2),
			new Definition(FAMILIES_MANAGE, "Families", "Add and edit families, guardians and contacts", 2),
			new Definition(STAFF_VIEW, "Staff", "View teacher and staff records", 2),
			new Definition(STAFF_MANAGE, "Staff", "Add and edit staff, documents, logins, leaving", 2),
			new Definition(STAFF_SALARY_VIEW, "Staff", "View salary details", 2),
			new Definition(STAFF_SALARY_MANAGE, "Staff", "Change salary details and increments", 2));

	/** Highest {@code since} in the catalogue; system roles below this version get new permissions on startup. */
	public static final int CATALOGUE_VERSION = ALL.stream().mapToInt(Definition::since).max().orElse(0);

	public static final Set<String> CODES = ALL.stream().map(Definition::code).collect(Collectors.toUnmodifiableSet());

	private Permissions() {
	}

	public static boolean exists(String code) {
		return CODES.contains(code);
	}

	public static List<String> codesSince(int versionExclusive) {
		return ALL.stream().filter(d -> d.since() > versionExclusive).map(Definition::code).toList();
	}

	static {
		if (CODES.size() != ALL.size()) {
			throw new IllegalStateException("Duplicate permission code in catalogue: " + Arrays.toString(ALL.toArray()));
		}
	}
}
