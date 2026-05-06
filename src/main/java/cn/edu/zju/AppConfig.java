package cn.edu.zju;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final AppConfig instance = new AppConfig();

    public static AppConfig getInstance() {
        return instance;
    }

    public AppConfig() {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("app.properties");
            Properties properties = new Properties();
            try {
                properties.load(resourceAsStream);
                this.jdbcUrl = properties.getProperty("jdbc.url");
                this.jdbcUsername = properties.getProperty("jdbc.username");
                this.jdbcPassword = properties.getProperty("jdbc.password");
                this.annovarPerl = properties.getProperty("annovar.perl", "perl");
                this.annovarTableAnnovar = properties.getProperty("annovar.table_annovar");
                this.annovarHumanDb = properties.getProperty("annovar.humandb");
                this.annovarBuildver = properties.getProperty("annovar.buildver", "hg19");
                this.annovarWorkdir = properties.getProperty("annovar.workdir",
                        System.getProperty("java.io.tmpdir") + "/dst-annovar");
            } catch (IOException e) {
                log.info("", e);
            }
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    log.info("", e);
                }
            }
        }
    }

    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String annovarPerl;
    private String annovarTableAnnovar;
    private String annovarHumanDb;
    private String annovarBuildver;
    private String annovarWorkdir;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getAnnovarPerl() {
        return annovarPerl;
    }

    public void setAnnovarPerl(String annovarPerl) {
        this.annovarPerl = annovarPerl;
    }

    public String getAnnovarTableAnnovar() {
        return annovarTableAnnovar;
    }

    public void setAnnovarTableAnnovar(String annovarTableAnnovar) {
        this.annovarTableAnnovar = annovarTableAnnovar;
    }

    public String getAnnovarHumanDb() {
        return annovarHumanDb;
    }

    public void setAnnovarHumanDb(String annovarHumanDb) {
        this.annovarHumanDb = annovarHumanDb;
    }

    public String getAnnovarBuildver() {
        return annovarBuildver;
    }

    public void setAnnovarBuildver(String annovarBuildver) {
        this.annovarBuildver = annovarBuildver;
    }

    public String getAnnovarWorkdir() {
        return annovarWorkdir;
    }

    public void setAnnovarWorkdir(String annovarWorkdir) {
        this.annovarWorkdir = annovarWorkdir;
    }
}
