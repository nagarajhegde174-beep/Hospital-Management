import { App } from './app';
import { AuthService } from './core/auth/auth.service';
import { UserProfileService } from './core/services/user-profile.service';

describe('App', () => {

  let authSpy: jasmine.SpyObj<AuthService>;
  let userProfileSpy: jasmine.SpyObj<UserProfileService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['isLoggedIn']);
    userProfileSpy = jasmine.createSpyObj('UserProfileService', ['loadName']);
    authSpy.isLoggedIn.and.returnValue(false);
  });

  it('should create the app', () => {
    const app = new App(authSpy, userProfileSpy);
    expect(app).toBeTruthy();
  });

  it('should load user name when already logged in', () => {
    authSpy.isLoggedIn.and.returnValue(true);
    const app = new App(authSpy, userProfileSpy);
    app.ngOnInit();
    expect(userProfileSpy.loadName).toHaveBeenCalled();
  });

});