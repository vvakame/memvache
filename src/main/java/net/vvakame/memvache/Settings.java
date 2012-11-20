package net.vvakame.memvache;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ユーザが行うMemvacheの設定を読み取る。
 * @author vvakame
 */
public class Settings {

	static final Logger logger = Logger.getLogger(Settings.class.getName());

	/** 超積極的にクエリをキャッシュした時のMemcache保持秒数 */
	int expireSecond = 300;

	/** Queryをキャッシュ"しない"Kindの一覧 */
	Set<String> ignoreKinds = Collections.emptySet();

	static Settings singleton;


	/**
	 * インスタンスを取得する。
	 * @return インスタンス
	 * @author vvakame
	 */
	public static Settings getInstance() {
		if (singleton == null) {
			singleton = new Settings();
		}
		return singleton;
	}

	Settings() {
		Properties properties = new Properties();
		try {
			properties.load(Settings.class.getResourceAsStream("/memvache.properties"));

			String expireSecondStr = properties.getProperty("expireSecond");
			if (expireSecondStr != null && !"".equals(expireSecondStr)) {
				expireSecond = Integer.parseInt(expireSecondStr);
			}

			String ignoreKindStr = properties.getProperty("ignoreKind");
			if (ignoreKindStr != null && !"".equals(ignoreKindStr)) {
				ignoreKinds = new HashSet<String>(Arrays.asList(ignoreKindStr.split(",")));
			} else {
				ignoreKinds = new HashSet<String>();
			}
		} catch (IOException e) {
			logger.log(Level.INFO, "", e);
		}
	}

	/**
	 * @return the expireSecond
	 * @category accessor
	 */
	public int getExpireSecond() {
		return expireSecond;
	}

	/**
	 * @param expireSecond the expireSecond to set
	 * @category accessor
	 */
	public void setExpireSecond(int expireSecond) {
		this.expireSecond = expireSecond;
	}

	/**
	 * @return the ignoreKinds
	 * @category accessor
	 */
	public Set<String> getIgnoreKinds() {
		return ignoreKinds;
	}

	/**
	 * @param ignoreKinds the ignoreKinds to set
	 * @category accessor
	 */
	public void setIgnoreKinds(Set<String> ignoreKinds) {
		this.ignoreKinds = ignoreKinds;
	}
}
