export ORACLE_SID="db_sid"
point=$1
echo $(sqlplus -s / as sysdba << EOF
shutdown immediate
startup mount
flashback database to restore point $point
/
alter database open resetlogs
/
exit
EOF)
