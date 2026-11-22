package mx.velian.angurgestion.auth.service;

import mx.velian.angurgestion.auth.dto.*;
import mx.velian.angurgestion.auth.model.UserAccount;

import java.util.List;

public interface AccountService {
    void register(RegistrationForm form);

    void createAccount(AccountForm form, long administratorId);

    void requestVerification(String email);

    void requestPasswordReset(String email);

    void requestAccess(String email);

    boolean isTokenValid(String token, String purpose);

    void verifyEmail(String token);

    void resetPassword(ResetPasswordForm form);

    List<UserAccount> accounts();

    void setActive(long id, boolean active, long administratorId);
}
