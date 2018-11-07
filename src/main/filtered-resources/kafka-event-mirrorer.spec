%define onms_is_snapshot %(echo '@project.version@' | grep -c -- -SNAPSHOT)

%{!?onms_build_timestamp:%define onms_build_timestamp %(date +"%%Y%%m%%d%%H%%M%%S")}
%{!?onms_version:%define onms_version %(echo '@project.version@' | sed -e 's,-SNAPSHOT,,')}
%{!?onms_instprefix:%define onms_instprefix /opt/%{name}}
%{!?onms_sysconfdir:%define onms_sysconfdir %{_sysconfdir}/sysconfig}
%{!?onms_java:%define onms_java jre-1.8.0}
%{!?onms_packagedir:%define onms_packagedir %{name}-@project.version@}

%if %onms_is_snapshot >= 1
%{!?onms_releasenumber:%define onms_releasenumber 0.%{onms_build_timestamp}.1}
%else
%{!?onms_releasenumber:%define onms_releasenumber 1}
%endif

Name: kafka-event-mirrorer
Summary: Forward OpenNMS Events Between Kafka Instances
Version: %{onms_version}
Release: %{onms_releasenumber}
License: AGPL
Group: Applications/System
BuildArch: noarch

Source: %{onms_packagedir}.tar.gz
URL: https://github.com/opennms-forge/kafka-event-mirrorer
BuildRoot: %{_tmppath}/%{name}-%{version}-root

Requires(pre): /usr/bin/getent
Requires(pre): /usr/sbin/groupadd
Requires(pre): /usr/sbin/useradd
Requires(pre): /sbin/nologin
Requires:      /sbin/nologin
Requires:      %{onms_java}

%description
This package contains a tool for forwarding OpenNMS events from one
Kafka instance to another, optionally filtering them.

%prep
%setup -n %{onms_packagedir}

%build
rm -rf "%{buildroot}"

%install

install -d -m 755 "%{buildroot}%{onms_instprefix}"
cp -pr bin lib "%{buildroot}%{onms_instprefix}/"

install -d -m 755 "%{buildroot}%{onms_sysconfdir}"
install -d -m 755 "%{buildroot}%{_initrddir}"
sed -e 's,@INSTPREFIX@,%{onms_instprefix},g' \
	-e 's,@SYSCONFDIR@,%{onms_sysconfdir},g' \
	-e 's,^.*PIDFILE=.*$,PIDFILE=/var/lib/%{name}/%{name}.pid,' \
	etc/%{name}.init > "%{buildroot}%{_initrddir}/%{name}"

sed -e 's,@INSTPREFIX@,%{onms_instprefix},g' \
	-e 's,@SYSCONFDIR@,%{onms_sysconfdir},g' \
	-e 's,^.*CONFFILE=.*$,CONFFILE=/etc/%{name}.yaml,' \
	-e 's,^.*PIDFILE=.*$,# PIDFILE=/var/lib/%{name}/%{name}.pid,' \
	-e 's,^.*RUNAS=.*$,RUNAS=opennms-kem,' \
	etc/%{name}.conf > "%{buildroot}%{onms_sysconfdir}/%{name}"

chmod 755 "%{buildroot}%{_initrddir}/%{name}"
chmod 644 "%{buildroot}%{onms_sysconfdir}/%{name}"

# Example Config
install -c -m 644 etc/example-config.yaml "%{buildroot}%{_sysconfdir}/%{name}.yaml"

%files
%defattr(664 opennms-kem opennms-kem 775)
%config(noreplace) %{onms_sysconfdir}/%{name}
%config(noreplace) %attr(640,opennms-kem,opennms-kem) %{_sysconfdir}/%{name}.yaml
%attr(755,opennms-kem,opennms-kem) %{_initrddir}/%{name}
%attr(755,opennms-kem,opennms-kem) %{onms_instprefix}/bin/*
%dir %{onms_instprefix}
%{onms_instprefix}

%pre
getent group opennms-kem >/dev/null || groupadd -r opennms-kem
getent passwd opennms-kem >/dev/null || \
	useradd -r -g opennms-kem -d "%{onms_instprefix}" -s /sbin/nologin \
	-c "OpenNMS Karaf Event Mirrorer" opennms-kem
install -d -m 755 -o opennms-kem -g opennms-kem /var/lib/%{name}
exit 0
