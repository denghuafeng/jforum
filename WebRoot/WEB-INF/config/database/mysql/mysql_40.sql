# @version: $Id$
# ################
# PermissionControl
# ################
PermissionControl.deleteAllRoleValues = DELETE jforum_role_values \
	FROM jforum_role_values rv, jforum_roles r \
	WHERE r.role_id = rv.role_id \
	AND r.group_id = ?
