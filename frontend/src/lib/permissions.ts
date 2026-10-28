/** Permission codes (mirror of backend shared.security.Permissions). The API checks every call; the UI only hides. */
export const P = {
  SCHOOL_PROFILE_VIEW: "school.profile.view",
  SCHOOL_PROFILE_EDIT: "school.profile.edit",
  SCHOOL_YEAR_MANAGE: "school.year.manage",
  SCHOOL_CALENDAR_MANAGE: "school.calendar.manage",
  IAM_USER_VIEW: "iam.user.view",
  IAM_USER_MANAGE: "iam.user.manage",
  IAM_ROLE_VIEW: "iam.role.view",
  IAM_ROLE_MANAGE: "iam.role.manage",
  IAM_LOGIN_HISTORY_VIEW: "iam.login_history.view",
  AUDIT_VIEW: "audit.view",
  SETTINGS_VIEW: "settings.view",
  SETTINGS_EDIT: "settings.edit",
  ACADEMICS_VIEW: "academics.view",
  ACADEMICS_MANAGE: "academics.manage",
  ADMISSIONS_VIEW: "admissions.view",
  ADMISSIONS_MANAGE: "admissions.manage",
  ADMISSIONS_ENROL: "admissions.enrol",
  STUDENTS_VIEW: "students.view",
  STUDENTS_MANAGE: "students.manage",
  STUDENTS_MOVE: "students.move",
  STUDENTS_PRIVATE_VIEW: "students.private.view",
  STUDENTS_PRINT: "students.print",
  FAMILIES_VIEW: "families.view",
  FAMILIES_MANAGE: "families.manage",
  STAFF_VIEW: "staff.view",
  STAFF_MANAGE: "staff.manage",
  STAFF_SALARY_VIEW: "staff.salary.view",
  STAFF_SALARY_MANAGE: "staff.salary.manage",
} as const;

export type Permission = (typeof P)[keyof typeof P];

export function can(permissions: readonly string[] | undefined, permission: Permission) {
  return permissions?.includes(permission) ?? false;
}
