package org.pentaho.platform.plugin.services.importer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.security.userroledao.AlreadyExistsException;
import org.pentaho.platform.api.engine.security.userroledao.IUserRoleDao;
import org.pentaho.platform.api.mimetype.IMimeType;
import org.pentaho.platform.api.mt.ITenant;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.importexport.RoleExport;
import org.pentaho.platform.plugin.services.importexport.UserExport;
import org.pentaho.platform.security.policy.rolebased.IRoleAuthorizationPolicyRoleBindingDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SolutionImportHandlerTest {

  SolutionImportHandler importHandler;
  IUserRoleDao userRoleDao;
  IRoleAuthorizationPolicyRoleBindingDao roleAuthorizationPolicyRoleBindingDao;

  @Before
  public void setUp() throws Exception {
    List<IMimeType> mimeTypes = new ArrayList<>();

    importHandler = new SolutionImportHandler( mimeTypes );
    userRoleDao = mock( IUserRoleDao.class );
    roleAuthorizationPolicyRoleBindingDao = mock( IRoleAuthorizationPolicyRoleBindingDao.class );

    PentahoSystem.registerObject( userRoleDao );
    PentahoSystem.registerObject( roleAuthorizationPolicyRoleBindingDao );
  }

  @Test
  public void testImportUsers_oneUserManyRoles() throws Exception {
    List<UserExport> users = new ArrayList<>();
    UserExport user = new UserExport();
    user.setUsername( "scrum master" );
    user.setRole( "coder" );
    user.setRole( "product owner" );
    user.setRole( "cat herder" );
    user.setPassword( "password" );
    users.add( user );

    Map<String, List<String>> rolesToUsers = importHandler.importUsers( users );

    assertEquals( 3, rolesToUsers.size() );
    assertEquals( "scrum master", rolesToUsers.get( "coder" ).get( 0 ) );
    assertEquals( "scrum master", rolesToUsers.get( "product owner" ).get( 0 ) );
    assertEquals( "scrum master", rolesToUsers.get( "cat herder" ).get( 0 ) );

    String[] strings = {};

    verify( userRoleDao ).createUser(
      any( ITenant.class ),
      eq( "scrum master" ),
      anyString(),
      anyString(),
      any( strings.getClass() ) );

    // should not set the password or roles explicitly if the createUser worked
    verify( userRoleDao, never() ).setUserRoles( any( ITenant.class ), anyString(), any( strings.getClass() ) );
    verify( userRoleDao, never() ).setPassword( any( ITenant.class ), anyString(), anyString() );
  }

  @Test
  public void testImportUsers_manyUserManyRoles() throws Exception {
    List<UserExport> users = new ArrayList<>();
    UserExport user = new UserExport();
    user.setUsername( "scrum master" );
    user.setRole( "coder" );
    user.setRole( "product owner" );
    user.setRole( "cat herder" );
    user.setPassword( "password" );
    users.add( user );

    UserExport user2 = new UserExport();
    user2.setUsername( "the dude" );
    user2.setRole( "coder" );
    user2.setRole( "awesome" );
    user2.setPassword( "password" );
    users.add( user2 );

    Map<String, List<String>> rolesToUsers = importHandler.importUsers( users );

    assertEquals( 4, rolesToUsers.size() );
    assertEquals( 2, rolesToUsers.get( "coder" ).size() );
    assertEquals( 1, rolesToUsers.get( "product owner" ).size() );
    assertEquals( "scrum master", rolesToUsers.get( "product owner" ).get( 0 ) );
    assertEquals( 1, rolesToUsers.get( "cat herder" ).size() );
    assertEquals( "scrum master", rolesToUsers.get( "cat herder" ).get( 0 ) );
    assertEquals( 1, rolesToUsers.get( "awesome" ).size() );
    assertEquals( "the dude", rolesToUsers.get( "awesome" ).get( 0 ) );

    String[] strings = {};

    verify( userRoleDao ).createUser(
      any( ITenant.class ),
      eq( "scrum master" ),
      anyString(),
      anyString(),
      any( strings.getClass() ) );

    verify( userRoleDao ).createUser(
      any( ITenant.class ),
      eq( "the dude" ),
      anyString(),
      anyString(),
      any( strings.getClass() ) );

    // should not set the password or roles explicitly if the createUser worked
    verify( userRoleDao, never() ).setUserRoles( any( ITenant.class ), anyString(), any( strings.getClass() ) );
    verify( userRoleDao, never() ).setPassword( any( ITenant.class ), anyString(), anyString() );
  }

  @Test
  public void testImportUsers_userAlreadyExists() throws Exception {
    List<UserExport> users = new ArrayList<>();
    UserExport user = new UserExport();
    user.setUsername( "scrum master" );
    user.setRole( "coder" );
    user.setPassword( "password" );
    users.add( user );
    String[] strings = {};

    when( userRoleDao.createUser(
      any( ITenant.class ),
      eq( "scrum master" ),
      anyString(),
      anyString(),
      any( strings.getClass() ) ) ).thenThrow( new AlreadyExistsException( "already there" ) );

    Map<String, List<String>> rolesToUsers = importHandler.importUsers( users );

    assertEquals( 1, rolesToUsers.size() );
    assertEquals( "scrum master", rolesToUsers.get( "coder" ).get( 0 ) );

    verify( userRoleDao ).createUser(
      any( ITenant.class ),
      eq( "scrum master" ),
      anyString(),
      anyString(),
      any( strings.getClass() ) );

    // should set the password or roles explicitly if the createUser failed
    verify( userRoleDao ).setUserRoles( any( ITenant.class ), anyString(), any( strings.getClass() ) );
    verify( userRoleDao ).setPassword( any( ITenant.class ), anyString(), anyString() );
  }

  @Test
  public void testImportRoles() throws Exception {
    String roleName = "ADMIN";
    List<String> permissions = new ArrayList<String>();

    RoleExport role = new RoleExport();
    role.setRolename( roleName );
    role.setPermission( permissions );

    List<RoleExport> roles = new ArrayList<>();
    roles.add( role );

    Map<String, List<String>> roleToUserMap = new HashMap<>();
    final List<String> adminUsers = new ArrayList<>();
    adminUsers.add( "admin" );
    adminUsers.add( "root" );
    roleToUserMap.put( roleName, adminUsers );

    String[] userStrings = adminUsers.toArray( new String[] {} );

    importHandler.importRoles( roles, roleToUserMap );

    verify( userRoleDao ).createRole( any( ITenant.class ), eq( roleName ), anyString(), any( userStrings.getClass() ) );
    verify( roleAuthorizationPolicyRoleBindingDao ).setRoleBindings( any( ITenant.class ), eq( roleName ), eq( permissions ) );
  }

  @Test
  public void testImportRoles_roleAlreadyExists() throws Exception {
    String roleName = "ADMIN";
    List<String> permissions = new ArrayList<String>();

    RoleExport role = new RoleExport();
    role.setRolename( roleName );
    role.setPermission( permissions );

    List<RoleExport> roles = new ArrayList<>();
    roles.add( role );

    Map<String, List<String>> roleToUserMap = new HashMap<>();
    final List<String> adminUsers = new ArrayList<>();
    adminUsers.add( "admin" );
    adminUsers.add( "root" );
    roleToUserMap.put( roleName, adminUsers );

    String[] userStrings = adminUsers.toArray( new String[] {} );

    when( userRoleDao.createRole( any( ITenant.class ), anyString(), anyString(), any( userStrings.getClass() ) ) )
      .thenThrow( new AlreadyExistsException( "already there" ) );

    importHandler.importRoles( roles, roleToUserMap );

    verify( userRoleDao ).createRole( any( ITenant.class ), anyString(), anyString(), any( userStrings.getClass() ) );

    // even if the roles exists, make sure we set the permissions on it anyway... they might have changed
    verify( roleAuthorizationPolicyRoleBindingDao ).setRoleBindings( any( ITenant.class ), eq( roleName ), eq(
      permissions ) );

  }

  @After
  public void tearDown() throws Exception {
    PentahoSystem.clearObjectFactory();
  }
}
