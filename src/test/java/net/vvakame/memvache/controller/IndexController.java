package net.vvakame.memvache.controller;

import net.vvakame.memvache.model.SampleModel;

import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import org.slim3.datastore.Datastore;

public class IndexController extends Controller {

	@Override
	protected Navigation run() throws Exception {

		{
			SampleModel model = new SampleModel();
			Datastore.put(model);
		}

		return null;
	}
}
