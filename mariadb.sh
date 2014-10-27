#!/bin/bash
#
# taken from https://github.com/Lukx/vagrant-lamp/blob/master/components/mariadb.sh
#
# apt: add mariadb sources and key
echo "deb http://mirror.netcologne.de/mariadb/repo/5.5/ubuntu trusty main" >> /etc/apt/sources.list
echo "deb-src http://mirror.netcologne.de/mariadb/repo/5.5/ubuntu trusty main" >> /etc/apt/sources.list

# mariadb issue: confer https://mariadb.com/kb/en/installing-mariadb-deb-files/#pinning-the-mariadb-repository
cat /dev/null > /etc/apt/preferences.d/mariadb
echo "Package: *" >> /etc/apt/preferences.d/mariadb
echo "Pin: origin mirror2.hs-esslingen.de"  >> /etc/apt/preferences.d/mariadb
echo "Pin-Priority: 1001"  >> /etc/apt/preferences.d/mariadb

apt-key adv --recv-keys --keyserver keyserver.ubuntu.com 0xcbcb082a1bb943db
apt-get update

apt-get install -y pwgen

# prepare for an unattended installation
export DEBIAN_FRONTEND=noninteractive
MYSQL_PASS=$(pwgen -s 12 1);
#MYSQL_VAGRANT_PASS=$(pwgen -s 8 1);
MYSQL_VAGRANT_PASS=vagrant

debconf-set-selections <<< "mariadb-server-5.5 mysql-server/root_password password $MYSQL_PASS"
debconf-set-selections <<< "mariadb-server-5.5 mysql-server/root_password_again password $MYSQL_PASS"

apt-get install -y --allow-unauthenticated mariadb-server mariadb-client

if [ -f $VAGRANT_SYNCED_DIR/vagrant/.mysql-passes ]
  then
    rm -f $VAGRANT_SYNCED_DIR/vagrant/.mysql-passes
fi

echo "root:${MYSQL_PASS}" >> ${VAGRANT_SYNCED_DIR}/vagrant/.mysql-passes
echo "vagrant:${MYSQL_VAGRANT_PASS}" >> ${VAGRANT_SYNCED_DIR}/vagrant/.mysql-passes

mysql -uroot -p$MYSQL_PASS -e "CREATE USER 'vagrant'@'localhost' IDENTIFIED BY '$MYSQL_VAGRANT_PASS'"

echo "MariaDB Root Passwords has been stored to .mysql-passes in your vagrant directory."
