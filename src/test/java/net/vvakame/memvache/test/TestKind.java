package net.vvakame.memvache.test;

import java.util.ArrayList;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;

/**
 * テスト用のKind.
 * @author vvakame
 */
@Model
public class TestKind {

	@Attribute(primaryKey = true)
	Key key;

	String str;

	String keyStr;

	@Attribute(lob = true)
	byte[] data;

	List<String> strList = new ArrayList<String>();


	/**
	 * @return the key
	 * @category accessor
	 */
	public Key getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 * @category accessor
	 */
	public void setKey(Key key) {
		this.key = key;
	}

	/**
	 * @return the str
	 * @category accessor
	 */
	public String getStr() {
		return str;
	}

	/**
	 * @param str the str to set
	 * @category accessor
	 */
	public void setStr(String str) {
		this.str = str;
	}

	/**
	 * @return the keyStr
	 * @category accessor
	 */
	public String getKeyStr() {
		return keyStr;
	}

	/**
	 * @param keyStr the keyStr to set
	 * @category accessor
	 */
	public void setKeyStr(String keyStr) {
		this.keyStr = keyStr;
	}

	/**
	 * @return the data
	 * @category accessor
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 * @category accessor
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * @return the strList
	 * @category accessor
	 */
	public List<String> getStrList() {
		return strList;
	}

	/**
	 * @param strList the strList to set
	 * @category accessor
	 */
	public void setStrList(List<String> strList) {
		this.strList = strList;
	}
}
