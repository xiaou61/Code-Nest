package com.xiaou.common.constant;

/**
 * 系统常量类
 *
 * @author xiaou
 */
public class Constants {

    /**
     * 系统相关常量
     */
    public static final String SYSTEM_NAME = "Code-Nest";
    public static final String SYSTEM_VERSION = "1.0.0";
    public static final String SYSTEM_AUTHOR = "xiaou";

    /**
     * 字符编码
     */
    public static final String UTF8 = "UTF-8";
    public static final String GBK = "GBK";

    /**
     * HTTP相关常量
     */
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_PUT = "PUT";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_PATCH = "PATCH";

    /**
     * Content-Type
     */
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";

    /**
     * 请求头常量
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_TOKEN = "Token";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * Token相关常量
     */
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_HEADER = "Authorization";
    public static final Long TOKEN_EXPIRE_TIME = 7200L; // 2小时，单位：秒

    /**
     * 缓存相关常量
     */
    public static final String CACHE_PREFIX = "code_nest:";
    public static final String CACHE_USER_PREFIX = CACHE_PREFIX + "user:";
    public static final String CACHE_TOKEN_PREFIX = CACHE_PREFIX + "token:";
    public static final String CACHE_CONFIG_PREFIX = CACHE_PREFIX + "config:";

    /**
     * 用户相关常量
     */
    public static final String DEFAULT_PASSWORD = "123456";
    public static final String ADMIN_USERNAME = "admin";
    public static final String GUEST_USERNAME = "guest";

    /**
     * 状态常量
     */
    public static final Integer STATUS_NORMAL = 0;
    public static final Integer STATUS_DISABLED = 1;
    public static final Integer STATUS_DELETED = 2;

    /**
     * 是否标识
     */
    public static final Integer YES = 1;
    public static final Integer NO = 0;

    /**
     * 性别常量
     */
    public static final Integer GENDER_UNKNOWN = 0;
    public static final Integer GENDER_MALE = 1;
    public static final Integer GENDER_FEMALE = 2;

    /**
     * 文件相关常量
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final Long MAX_FILE_SIZE = 100 * 1024 * 1024L; // 100MB
    public static final String[] ALLOWED_IMAGE_TYPES = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
    public static final String[] ALLOWED_DOCUMENT_TYPES = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"};

    /**
     * 日期时间格式常量
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 分页相关常量
     */
    public static final Integer DEFAULT_PAGE_NUM = 1;
    public static final Integer DEFAULT_PAGE_SIZE = 10;
    public static final Integer MAX_PAGE_SIZE = 1000;

    /**
     * 正则表达式常量
     */
    public static final String REGEX_EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String REGEX_PHONE = "^1[3-9]\\d{9}$";
    public static final String REGEX_ID_CARD = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";
    public static final String REGEX_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$";

    /**
     * 私有构造器，防止实例化
     */
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
