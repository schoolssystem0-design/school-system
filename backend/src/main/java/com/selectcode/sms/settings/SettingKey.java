package com.selectcode.sms.settings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The settings catalogue (FR 12): every configurable school rule, with its type and default. Modules read values
 * through {@link SettingsService}, so one rule is applied the same way everywhere (FR 12.3).
 *
 * <p>Keys are defined up front even where the module that uses them arrives in a later phase; the UI shows them so
 * schools can prepare their policies.
 */
public enum SettingKey {

	// --- General preferences (FR 1.8)
	MESSAGE_SIGNATURE(Group.GENERAL, "Message signature", "Text added to the end of every SMS / WhatsApp message",
			Type.TEXT, "Administration", null),
	DATE_FORMAT(Group.GENERAL, "Date format", "How dates are printed on documents", Type.ENUM, "DD-MM-YYYY",
			List.of("DD-MM-YYYY", "DD/MM/YYYY", "YYYY-MM-DD", "D MMM YYYY")),
	MASK_DASHBOARD_FIGURES(Group.GENERAL, "Mask dashboard figures",
			"Hide sensitive totals on the dashboard when the screen may be seen by visitors", Type.BOOLEAN, false,
			null),

	// --- Attendance (FR 13)
	STUDENT_LATE_TIME_1(Group.ATTENDANCE, "Student late time (slot 1)",
			"Students arriving after this time are marked late", Type.TIME, "08:00", null),
	STUDENT_LATE_TIME_2(Group.ATTENDANCE, "Student late time (slot 2)",
			"Second late time for classes with a different start time", Type.TIME, "08:30", null),
	TEACHING_STAFF_LATE_TIME(Group.ATTENDANCE, "Teaching staff late time", "Teachers arriving after this are late",
			Type.TIME, "07:30", null),
	OTHER_STAFF_LATE_TIME(Group.ATTENDANCE, "Other staff late time", "Non-teaching staff arriving after this are late",
			Type.TIME, "07:00", null),
	REPEATED_ABSENCE_DAYS(Group.ATTENDANCE, "Repeated absence alert (days in a row)",
			"Flag students absent this many working days in a row", Type.INTEGER, 3, null),
	LOW_ATTENDANCE_PERCENT(Group.ATTENDANCE, "Low attendance alert (%)",
			"Flag students whose attendance falls below this percentage", Type.INTEGER, 75, null),

	// --- Fees (FR 10)
	FEE_DUE_DAY(Group.FEES, "Fee due day of month", "Monthly fee vouchers are due on this day", Type.INTEGER, 10,
			null),
	LATE_FEE_AMOUNT(Group.FEES, "Late fee (Rs.)", "Fine added to unpaid bills after the due date", Type.MONEY, "200",
			null),
	VOUCHER_VALIDITY_DAYS(Group.FEES, "Voucher validity (days after due date)",
			"Banks accept the voucher until this many days after the due date", Type.INTEGER, 10, null),
	SHOW_ARREARS_ON_VOUCHER(Group.FEES, "Show arrears on vouchers", "Print previous unpaid amounts separately",
			Type.BOOLEAN, true, null),
	SHOW_DISCOUNT_ON_VOUCHER(Group.FEES, "Show discounts on vouchers", "Print the discount line on vouchers",
			Type.BOOLEAN, true, null),
	SIGNATURE_ON_VOUCHER(Group.FEES, "Signature on vouchers", "Print the accounts signature on vouchers", Type.BOOLEAN,
			false, null),

	// --- Discounts (FR 10.3)
	MAX_DISCOUNT_WITHOUT_APPROVAL(Group.DISCOUNTS, "Maximum discount without approval (%)",
			"Larger discounts wait for the principal's approval", Type.INTEGER, 25, null),

	// --- Receipts (FR 10.8)
	RECEIPT_STYLE(Group.RECEIPTS, "Receipt size", "Paper size for printed receipts", Type.ENUM, "A5",
			List.of("A4", "A5")),
	SEND_RECEIPT_SMS(Group.RECEIPTS, "Send receipt SMS", "Send an SMS to the family when a payment is saved",
			Type.BOOLEAN, true, null),

	// --- Report cards (FR 14.13)
	SHOW_ATTENDANCE_ON_REPORT_CARD(Group.REPORT_CARDS, "Attendance % on report cards",
			"Print the attendance percentage on report cards", Type.BOOLEAN, true, null),
	SHOW_HIGHEST_MARKS_COLUMN(Group.REPORT_CARDS, "Highest marks column",
			"Print the highest marks in class for each subject", Type.BOOLEAN, false, null),

	// --- Results (FR 14.8)
	SHOW_CLASS_POSITIONS(Group.RESULTS, "Show class positions",
			"Positions on report cards, result sheets and result SMS (all together)", Type.BOOLEAN, false, null),
	POSITION_TIE_METHOD(Group.RESULTS, "Position ties", "How equal totals are ranked", Type.ENUM, "STANDARD",
			List.of("STANDARD", "DENSE")),

	// --- Signatures and certificates (FR 5.8, 14)
	PRINCIPAL_SIGNATURE_ON_MARKSHEET(Group.SIGNATURES, "Principal signature on marksheets",
			"Print the principal's signature on report cards and marksheets", Type.BOOLEAN, true, null),
	SIGNATURE_ON_CERTIFICATES(Group.CERTIFICATES, "Signatures on certificates",
			"Print signatures on leaving, character, experience and provisional certificates", Type.BOOLEAN, true,
			null),

	// --- Warning letters
	MAX_WARNINGS_STUDENT(Group.WARNINGS, "Maximum warnings (students)",
			"Warnings before a final notice for a student", Type.INTEGER, 3, null),
	MAX_WARNINGS_STAFF(Group.WARNINGS, "Maximum warnings (staff)", "Warnings before a final notice for staff",
			Type.INTEGER, 3, null),

	// --- Messages (FR 8.6)
	MESSAGE_CHANNEL(Group.MESSAGES, "Messaging method", "Channel used for messages to families", Type.ENUM, "SMS",
			List.of("SMS", "WHATSAPP")),
	SMS_SENDER_MASK(Group.MESSAGES, "SMS sender name", "Approved sender ID shown on SMS (set when the account is opened)",
			Type.TEXT, "", null),
	ADMIN_ALERT_MOBILE(Group.MESSAGES, "Admin mobile for alerts", "Mobile number that receives system alerts",
			Type.TEXT, "", null),

	// --- Salary (FR 13.12, DECISIONS D-04)
	LATE_DAYS_PER_ABSENCE(Group.SALARY, "Late days counted as one absence",
			"For salary deduction; 0 = late days are not deducted", Type.INTEGER, 3, null),
	DEDUCT_ABSENT_DAYS(Group.SALARY, "Deduct absent days from salary", "Unpaid absent days reduce the salary",
			Type.BOOLEAN, true, null),
	HALF_DAY_DEDUCTION(Group.SALARY, "Deduct half days", "Half days reduce the salary by half a day", Type.BOOLEAN,
			true, null);

	public enum Group {

		GENERAL("General preferences"), ATTENDANCE("Attendance"), FEES("Fees"), DISCOUNTS("Discounts"),
		RECEIPTS("Receipts"), REPORT_CARDS("Report cards"), RESULTS("Results"), SIGNATURES("Signatures"),
		CERTIFICATES("Certificates"), WARNINGS("Warning letters"), MESSAGES("Messages"), SALARY("Salary");

		public final String label;

		Group(String label) {
			this.label = label;
		}
	}

	public enum Type {
		BOOLEAN, INTEGER, MONEY, TEXT, TIME, ENUM
	}

	public final Group group;

	public final String label;

	public final String description;

	public final Type type;

	public final Object defaultValue;

	public final List<String> options;

	SettingKey(Group group, String label, String description, Type type, Object defaultValue, List<String> options) {
		this.group = group;
		this.label = label;
		this.description = description;
		this.type = type;
		this.defaultValue = defaultValue;
		this.options = options == null ? List.of() : options;
	}

	/** The storage key, e.g. {@code attendance.student_late_time_1}. */
	public String key() {
		return group.name().toLowerCase() + "." + name().toLowerCase();
	}

	public static Optional<SettingKey> fromKey(String key) {
		return Arrays.stream(values()).filter(k -> k.key().equals(key)).findFirst();
	}
}
