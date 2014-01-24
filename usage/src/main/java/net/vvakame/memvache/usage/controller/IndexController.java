package net.vvakame.memvache.usage.controller;

import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;

/**
 * Index.
 * @author vvakame
 */
public class IndexController extends Controller {

	@Override
	protected Navigation run() throws Exception {
		response.getWriter().println("hello, world!");

		return null;
	}
}
