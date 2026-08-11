create table FIPSSessionSettings (
	mvccVersion LONG default 0 not null,
	fipsSessionSettingsId LONG not null primary key,
	companyId LONG,
	userId LONG,
	userName VARCHAR(75) null,
	createDate DATE null,
	modifiedDate DATE null,
	absoluteLifetimeMinutes INTEGER,
	idleTimeoutMinutes INTEGER
);