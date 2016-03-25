package models;

import models.utils.AppException;
import models.utils.Hash;
import play.data.format.Formats;
import play.data.validation.Constraints;
import com.avaje.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: yesnault
 * Date: 20/01/12
 */
@Entity
public class User extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String email;

    @Column(length = 100, nullable = true)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(100)
    public String firstname;

    @Column(length = 100, nullable = true)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(100)
    public String lastname;

    @Column(length = 100, nullable = true)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(100)
    public String country;


    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String username;

    public String confirmationtoken;

    @Constraints.Required
    @Formats.NonEmpty
    public String passwordhash;

    @Formats.DateTime(pattern = "dd-MM-yyyy")
    public Date dateCreation;

    @Formats.NonEmpty
    public Boolean validated = false;

    @Formats.NonEmpty
    public Boolean admin = false;

    public String auth_key;

    public void deleteAuth_key() {
        this.auth_key = null;
        save();
    }

    // -- Queries (long id, user.class)
    public static Model.Finder<Long, User> find = new Model.Finder<Long, User>(Long.class, User.class);

    /**
     * Retrieve a user from an email.
     *
     * @param email email to search
     * @return a user
     */
    public static User findByEmail(String email) {
        return find.where().eq("email", email).findUnique();
    }

    /**
     * Retrieve a user from a username.
     *
     * @param userName Full name
     * @return a user
     */
    public static User findByUserName(String userName) {
        return find.where().eq("username", userName).findUnique();
    }

    /**
     * Retrieves a user from a confirmation token.
     *
     * @param token the confirmation token to use.
     * @return a user if the confirmation token is found, null otherwise.
     */
    public static User findByConfirmationToken(String token) {
        return find.where().eq("confirmationtoken", token).findUnique();
    }

    /**
     * Authenticate a User, from a email and clear password.
     *
     * @param email         email
     * @param clearPassword clear password
     * @return User if authenticated, null otherwise
     * @throws AppException App Exception
     */
    public static User authenticate(String email, String clearPassword) throws AppException {

        // get the user with email only to keep the salt password
        User user = find.where().eq("email", email).findUnique();
        if (user != null) {
            // get the hash password from the salt + clear password
            if (Hash.checkPassword(clearPassword, user.passwordhash)) {
                return user;
            }
        }
        else{
            user = find.where().eq("username", email).findUnique();
            if (user != null) {
                // get the hash password from the salt + clear password
                if (Hash.checkPassword(clearPassword, user.passwordhash)) {
                    return user;
                }
            }
        }
        return null;
    }

    public void changePassword(String password) throws AppException {
        this.passwordhash = Hash.createPassword(password);
        this.save();
    }

    /**
     * Confirms an account.
     *
     * @return true if confirmed, false otherwise.
     * @throws AppException App Exception
     */
    public static boolean confirm(User user) throws AppException {
        if (user == null) {
            return false;
        }

        user.confirmationtoken = null;
        user.validated = true;
        user.save();
        return true;
    }

    public static User findByAuthKey(String auth_key) {
        if (auth_key == null) {
            return null;
        }

        try  {
            return find.where().eq("auth_key", auth_key).findUnique();
        }
        catch (Exception e) {
            return null;
        }
    }
    public static class SignUpUserResponseBuilder {
        private String email;
        private String country;
        private String username;
        private String createdon;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCreatedon() {
            return createdon;
        }

        public void setCreatedon(String createdon) {
            this.createdon = createdon;
        }

        public String getAuth_key() {
            return auth_key;
        }

        public void setAuth_key(String auth_key) {
            this.auth_key = auth_key;
        }

        private String auth_key;

        private String getDate(Date date) {
            SimpleDateFormat formatter;
            formatter = new SimpleDateFormat("dd-MM-yyyy");
            return formatter.format(date);
        }
        public SignUpUserResponseBuilder(User user){
            this.email = user.email;
            this.country = user.country;
            this.auth_key = user.auth_key;
            this.createdon = getDate(user.dateCreation);
            this.username = user.username;
        }
    }
}
