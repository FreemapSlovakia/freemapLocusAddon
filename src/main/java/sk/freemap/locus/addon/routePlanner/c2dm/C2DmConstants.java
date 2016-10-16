package sk.freemap.locus.addon.routePlanner.c2dm;

/**
 *
 *
 * @author <a href="mailto:m.zdila@mwaysolutions.com">Martin Ždila</a>
 */
public interface C2DmConstants {

	String REGISTRATION_ID = "registration_id";
	String EXTRA_COLLAPSE_KEY = "collapse_key";
	String EXTRA_FROM = "from";
	String EXTRA_SENDER = "sender";
	String EXTRA_APP = "app";
	String EXTRA_ERROR = "error";
	String EXTRA_UNREGISTERED = "unregistered";

	String ACTION_REGISTER = "com.google.android.c2dm.intent.REGISTER";
	String ACTION_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
	String ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";

	String ERROR_PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";
	String ERROR_INVALID_SENDER = "INVALID_SENDER";
	String ERROR_TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";
	String ERROR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
	String ERROR_ACCOUNT_MISSING = "ACCOUNT_MISSING";
	String ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";

}
