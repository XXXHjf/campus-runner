package com.mikasa.campusrunner.common.constant;

/**
 * author  Edith
 * created  2024/4/20 13:52
 */

public class MessageConstant {
    public static final String LOGIN_FAILED = "Login failed";
    public static final String AUTHEN_SUCCESS = "Authentication successful";
    public static final String AUTHEN_FAILED = "Authentication failed";
    public static final String DUPLICATE_RESERVE_ADDRESS = "REPEAT";
    public static final String EMPTY_RESERVE_ADDRESS = "Cannot add an empty address";
    public static final String FILE_UPLOAD_FAILED = "File upload failed";

    public static final String NOT_FOUND_ADDRESS = "Address not found";

    public static final String NOT_FOUND_PARAM = "Missing required parameters";

    public static final String NOT_FOUND_ORDER = "Order not found";

    public static final String ORDER_ALREADY_TAKE = "This order has already been taken";

    public static final String ORDER_ALREADY_CANCEL = "This order has already been canceled";

    public static final String TAKE_ORDER_NOT_FOUND = "The order has not been taken or has been deleted";

    public static final String NOT_FOUND_TAKE_ORDER = "Take-order record not found";

    public static final String NO_CANCELR_EASON = "Missing cancellation reason";

    public static final String NOT_YOUR_ORDER = "You cannot modify someone else's order";

    public static final String NO_IMAGE = "No image proof of order completion";

    public static final String NOT_THIS_ADDRESS_TYPE = "Address type mismatch, not the pickup or delivery address you are looking for";
    public static final String STATUS_NOT_WAIT_TO_TAKE_ORDER = "Order status is not 'waiting to be taken'";
    public static final String STATUS_NOT_ALREADY_TAKE_ORDER = "Order status is not 'taken', cannot send message";
    public static final String STATUS_NOT_DELIVERYING = "Order status is not 'delivering', cannot send message";
    public static final String STATUS_NOT_FINISHED = "Order has not been delivered yet";
    public static final String ORDER_TIME_OUT_TO_AUTO_CANCEL = "Order auto-canceled due to timeout";

    public static final String ORDER_TIME_OUT_NOT_PAY = "Order canceled due to unpaid timeout";

    public static final String ORDER_NOT_FINISHED = "Order is not finished yet, photo not available";

    public static final String USER_NOT_AUTHEN = "User not authenticated";

    public static final String NO_NOT_PAY_ORDER_WITH_ORDERID = "No unpaid order found for this order ID";
    public static final String ORDER_NOT_PAY = "Order not paid, cannot withdraw";
    public static final String ORDER_STATE_NOT_CONFIRMS_OR_WITHDRAWAL_FAILED = "Order is not in confirmed or withdrawal-failed state";
    public static final String ORDER_CHECK_PAY_FAIL = "Order status check failed, auto-canceled";
    public static final String NO_STUDENT_ID_CARD = "Student ID card image not provided, please upload";
    public static final String NO_ADMIN_USER = "Invalid credentials, admin user not found";
    public static final String NO_USER = "User not found";
    public static final String NO_SCHOOL_NAME = "School name not found, please try again";
    public static final String ORDER_TIME_OUT_TO_AUTO_REFUND = "Order timed out with no one taking it, auto-refunding";
}
