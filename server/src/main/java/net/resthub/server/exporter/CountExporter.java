package net.resthub.server.exporter;

import net.resthub.server.cache.CcCount;
import net.resthub.server.handler.CountHandler;

import org.hibernate.Session;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class CountExporter extends Exporter<CcCount> {

	@Inject
	public CountExporter(@Assisted CountHandler handler) {
		super(handler);
	}

	@Override
	protected CcCount retrieveData(Session session) throws Exception {
		return getDf().getCount(session, (CountHandler) getHandler());
	}

}
