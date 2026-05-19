create index IX_E7F5A093 on Audit_AuditEvent (companyId, accountEntryId);
create index IX_75DFC845 on Audit_AuditEvent (companyId, scope[$COLUMN_LENGTH:75$]);