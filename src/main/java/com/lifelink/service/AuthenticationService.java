package com.lifelink.service;

import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.*;
import com.lifelink.util.PasswordUtil;
import com.lifelink.util.ValidationUtil;

import java.util.Optional;

/**
 * Handles registration and login for all three roles. Controllers call
 * into this service rather than touching the repository or hashing
 * logic directly (per the architecture rule: no business logic in
 * controllers).
 */
public class AuthenticationService {

    private final UserRepository userRepository = new UserRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    public static class AuthResult {
        public final boolean success;
        public final String message;
        public final User user;

        private AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        static AuthResult ok(User user) {
            return new AuthResult(true, "Success", user);
        }

        static AuthResult fail(String message) {
            return new AuthResult(false, message, null);
        }
    }

    public AuthResult login(String email, String password) {
        if (!ValidationUtil.isNotBlank(email) || !ValidationUtil.isNotBlank(password)) {
            return AuthResult.fail("Please enter both email and password.");
        }
        try {
            Optional<User> found = userRepository.findByEmail(normalizeEmail(email));
            if (found.isEmpty()) {
                return AuthResult.fail("No account found with that email.");
            }
            User user = found.get();
            if (!PasswordUtil.verify(password, user.getPasswordHash())) {
                return AuthResult.fail("Incorrect password.");
            }
            activityLogRepository.log(user.getId(), "LOGIN", user.getName() + " logged in.");
            return AuthResult.ok(user);
        } catch (IllegalArgumentException e) {
            return AuthResult.fail("This account has invalid login data. Please create the account again.");
        } catch (RuntimeException e) {
            return AuthResult.fail("Unable to sign in right now. Please try again.");
        }
    }

    public AuthResult registerDonor(String name, String email, String password, String phone,
                                     String location, BloodType bloodType, int age, double weight) {
        AuthResult common = validateCommonFields(name, email, password, phone, location);
        if (common != null) return common;

        if (!ValidationUtil.isValidAge(age)) {
            return AuthResult.fail("Age must be between 16 and 70 for donor eligibility.");
        }
        if (!ValidationUtil.isValidWeight(weight)) {
            return AuthResult.fail("Weight must be at least 45 kg for donor eligibility.");
        }
        if (bloodType == null) {
            return AuthResult.fail("Please select a blood type.");
        }

        Donor donor = new Donor();
        donor.setName(name.trim());
        donor.setEmail(normalizeEmail(email));
        donor.setPasswordHash(PasswordUtil.hash(password));
        donor.setPhone(phone.trim());
        donor.setLocation(location.trim());
        donor.setBloodType(bloodType);
        donor.setAge(age);
        donor.setWeight(weight);
        donor.setLastDonationDate(null);
        donor.setAvailable(true);

        User saved = userRepository.save(donor);
        activityLogRepository.log(saved.getId(), "REGISTER", "Donor registered: " + saved.getName());
        return AuthResult.ok(saved);
    }

    public AuthResult registerRecipient(String name, String email, String password, String phone, String location) {
        AuthResult common = validateCommonFields(name, email, password, phone, location);
        if (common != null) return common;

        Recipient recipient = new Recipient();
        recipient.setName(name.trim());
        recipient.setEmail(normalizeEmail(email));
        recipient.setPasswordHash(PasswordUtil.hash(password));
        recipient.setPhone(phone.trim());
        recipient.setLocation(location.trim());

        User saved = userRepository.save(recipient);
        activityLogRepository.log(saved.getId(), "REGISTER", "Recipient registered: " + saved.getName());
        return AuthResult.ok(saved);
    }

    public AuthResult registerBloodBank(String name, String email, String password, String phone,
                                         String location, String address) {
        AuthResult common = validateCommonFields(name, email, password, phone, location);
        if (common != null) return common;
        if (!ValidationUtil.isNotBlank(address)) {
            return AuthResult.fail("Please enter the blood bank's address.");
        }

        BloodBank bank = new BloodBank();
        bank.setName(name.trim());
        bank.setEmail(normalizeEmail(email));
        bank.setPasswordHash(PasswordUtil.hash(password));
        bank.setPhone(phone.trim());
        bank.setLocation(location.trim());
        bank.setAddress(address.trim());

        User saved = userRepository.save(bank);
        activityLogRepository.log(saved.getId(), "REGISTER", "Blood bank registered: " + saved.getName());
        return AuthResult.ok(saved);
    }

    private AuthResult validateCommonFields(String name, String email, String password, String phone, String location) {
        if (!ValidationUtil.isNotBlank(name)) return AuthResult.fail("Please enter your name.");
        if (!ValidationUtil.isValidEmail(email)) return AuthResult.fail("Please enter a valid email address.");
        if (password == null || password.length() < 6) return AuthResult.fail("Password must be at least 6 characters.");
        if (!ValidationUtil.isValidPhone(phone)) return AuthResult.fail("Please enter a valid phone number.");
        if (!ValidationUtil.isNotBlank(location)) return AuthResult.fail("Please enter your location.");
        if (userRepository.emailExists(normalizeEmail(email))) return AuthResult.fail("An account with this email already exists.");
        return null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
