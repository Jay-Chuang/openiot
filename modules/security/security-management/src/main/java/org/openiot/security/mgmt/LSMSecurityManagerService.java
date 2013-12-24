/**
 * Copyright (c) 2011-2014, OpenIoT
 *
 * This library is free software; you can redistribute it and/or
 * modify it either under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation
 * (the "LGPL"). If you do not alter this
 * notice, a recipient may use your version of this file under the LGPL.
 *
 * You should have received a copy of the LGPL along with this library
 * in the file COPYING-LGPL-2.1; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * OF ANY KIND, either express or implied. See the LGPL  for
 * the specific language governing rights and limitations.
 *
 * Contact: OpenIoT mailto: info@openiot.eu
 */

package org.openiot.security.mgmt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.jasig.cas.services.RegisteredService;
import org.openiot.lsm.security.oauth.LSMOAuthHttpManager;
import org.openiot.lsm.security.oauth.mgmt.Permission;
import org.openiot.lsm.security.oauth.mgmt.Role;
import org.openiot.lsm.security.oauth.mgmt.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * This class is responsible for retrieving and persisting OpenIoT authentication and access control
 * management objects.
 * 
 * @author Mehdi Riahi
 * 
 */
@ManagedBean(name = "securityManagerService")
@ApplicationScoped
public class LSMSecurityManagerService implements Serializable, SecurityManagerService {

	private static final long serialVersionUID = -2254514562625584422L;

	private static Logger logger = LoggerFactory.getLogger(LSMSecurityManagerService.class);

	private final HashSet<String> filteredServices;

	static String OAuthGraphURL = "http://lsm.deri.ie/OpenIoT/OAuth#";
	private String lSMOauthGraphURL = OAuthGraphURL;

	LSMOAuthHttpManager lsmOAuthHttpManager = new LSMOAuthHttpManager(OAuthGraphURL);

	public LSMSecurityManagerService() {
		filteredServices = new HashSet<String>();
		filteredServices.add("Service Manager");
		filteredServices.add("HTTP");
	}

	public Permission getPermission(String perId) {
		return lsmOAuthHttpManager.getPermission(perId);
	}

	public void deletePermission(String perId) {
		lsmOAuthHttpManager.deletePermission(perId);
	}

	public void addPermission(Permission permission) {
		lsmOAuthHttpManager.addPermission(permission);

	}

	public Role getRole(String roleId) {
		return lsmOAuthHttpManager.getRole(roleId);
	}

	public void deleteRole(String roleId) {
		lsmOAuthHttpManager.deleteRole(roleId);
	}

	public void addRole(Role role) {
		lsmOAuthHttpManager.addRole(role);
	}

	public User getUser(String userId) {
		return lsmOAuthHttpManager.getUser(userId);
	}

	public void deleteUser(String userId) {
		lsmOAuthHttpManager.deleteUser(userId);
	}

	public void addUser(User user) {
		if (user.getRoles() == null)
			user.setRoles(new ArrayList<Role>());
		lsmOAuthHttpManager.addUser(user);
	}

	/**
	 * Retrieves a user by the username
	 * 
	 * @param username
	 * @return
	 */
	public User getUserByUsername(String username) {
		org.openiot.lsm.security.oauth.mgmt.User user = null;
		String userURL = "http://lsm.deri.ie/resource/user/" + username;
		if (username.contains("http://lsm.deri.ie/resource/user/")) {
			userURL = username;
			username = username.substring(username.lastIndexOf("/") + 1);
		}
		String sparql = " select ?nick ?mbox ?pass ?role" + " from <" + lSMOauthGraphURL + "> \n" + "where{ " + "<" + userURL
				+ "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/User>." + "OPTIONAL{<" + userURL
				+ "> <http://xmlns.com/foaf/0.1/nick> ?nick.}" + "OPTIONAL{<" + userURL + "> <http://xmlns.com/foaf/0.1/mbox> ?mbox.}" + "<" + userURL
				+ "> <http://openiot.eu/ontology/ns/password> ?pass." + "<" + userURL + "> <http://openiot.eu/ontology/ns/role> ?role." + "}";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
			if (results.hasNext()) {
				user = new org.openiot.lsm.security.oauth.mgmt.User();
				user.setUsername(username);
				while (results.hasNext()) {
					QuerySolution soln = results.nextSolution();
					user.setEmail(soln.get("?mbox").toString());
					user.setPassword(soln.get("?pass").toString());
					user.setName(soln.get("?nick").toString());
					List<Role> roles = user.getRoles();
					if (roles == null) {
						roles = new ArrayList<Role>();
						user.setRoles(roles);
					}
					Role role = getRole(soln.get("?role").toString());
					if (!roles.contains(role)) {
						roles.add(role);
					}
				}
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return user;
	}

	/**
	 * Retrieves a user by the email
	 * 
	 * @param email
	 * @return
	 */
	public User getUserByEmail(String email) {
		org.openiot.lsm.security.oauth.mgmt.User user = null;

		String sparql = " select ?userId" + " from <" + lSMOauthGraphURL + "> \n" + "where{ "
				+ " ?userId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/User>."
				+ " ?userId <http://xmlns.com/foaf/0.1/mbox> \"" + email + "\"}";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();

			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				user = getUser(soln.get("?userId").toString());
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return user;
	}

	public List<Role> getAllRoles() {
		List<Role> roleList = null;
		String sparql = " select ?roleId" + " from <" + lSMOauthGraphURL + "> \n" + "where{ "
				+ "?roleId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/ClientRole>" + "}";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
			roleList = new ArrayList<Role>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				String roleId = soln.get("?roleId").toString();
				Role role = getRole(roleId);
				roleList.add(role);
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return roleList;
	}

	public List<User> getRoleUsers(Role role) {
		List<User> userList = null;
		String roleId = "http://lsm.deri.ie/resource/role/" + role.getName();
		String sparql = " select ?userId" + " from <" + lSMOauthGraphURL + "> \n" + "where{ " + " ?userId <http://openiot.eu/ontology/ns/role> <" + roleId
				+ "> " + "}";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
			userList = new ArrayList<User>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				String userId = soln.get("?userId").toString();
				User user = getUser(userId);
				userList.add(user);
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return userList;
	}

	public List<User> getAllUsers() {
		List<User> userList = null;
		String sparql = " select ?userId" + " from <" + lSMOauthGraphURL + "> \n" + "where{ "
				+ " ?userId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/User> }";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
			userList = new ArrayList<User>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				String userId = soln.get("?userId").toString();
				User user = getUser(userId);
				userList.add(user);
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return userList;
	}

	public List<Permission> getAllPermissions() {
		List<Permission> permissionList = null;
		String sparql = " select ?permId" + " from <" + lSMOauthGraphURL + "> \n" + "where{ "
				+ " ?permId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/ClientPermission> }";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
			permissionList = new ArrayList<Permission>();
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				String userId = soln.get("?permId").toString();
				Permission perm = getPermission(userId);
				permissionList.add(perm);
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return permissionList;
	}

	@Override
	public List<RegisteredService> getAllServices() {
		final List<RegisteredService> allRegisteredServices = lsmOAuthHttpManager.getAllRegisteredServices();
		List<RegisteredService> filteredList = new ArrayList<RegisteredService>();
		for (RegisteredService service : allRegisteredServices) {
			if (!filteredServices.contains(service.getName()))
				filteredList.add(service);
		}
		return filteredList;
	}

	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract User getUserById(Long userId);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract List<Role> getAllRoles();
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract List<Permission> getAllPermissions();
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean addRoleToUser(User user, Role role);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean addPermissionToRole(Role role, Permission permission, Long
	// serviceId);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean saveUser(User user, char[] passwd);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean savePermission(Permission permission);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean saveRole(Role role);
}