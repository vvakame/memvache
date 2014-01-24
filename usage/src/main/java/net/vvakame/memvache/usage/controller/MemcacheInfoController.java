package net.vvakame.memvache.usage.controller;

import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import org.slim3.memcache.Memcache;

import com.google.appengine.api.memcache.Stats;

/**
 * Show memcache info
 * @author vvakame
 */
public class MemcacheInfoController extends Controller {

	@Override
	protected Navigation run() throws Exception {
		Stats stats = Memcache.statistics();
		long itemCount = stats.getItemCount();
		response.getOutputStream().write(String.valueOf(itemCount).getBytes());

		return null;
	}
}
