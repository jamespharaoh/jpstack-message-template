package wbs.platform.exception.hibernate;

import static wbs.framework.utils.etc.NumberUtils.toJavaIntegerRequired;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Instant;

import lombok.NonNull;
import wbs.framework.hibernate.HibernateDao;
import wbs.platform.exception.model.ExceptionLogDao;
import wbs.platform.exception.model.ExceptionLogRec;
import wbs.platform.exception.model.ExceptionLogSearch;
import wbs.platform.exception.model.ExceptionLogTypeRec;
import wbs.platform.user.model.UserRec;

public
class ExceptionLogDaoHibernate
	extends HibernateDao
	implements ExceptionLogDao {

	@Override
	public
	Long countWithAlert () {

		return findOne (
			"countWithAlert ()",
			Long.class,

			createCriteria (
				ExceptionLogRec.class,
				"_exceptionLog")

			.add (
				Restrictions.eq (
					"_exceptionLog.alert",
					true))

			.setProjection (
				Projections.rowCount ())

		);

	}

	@Override
	public
	Long countWithAlertAndFatal () {

		return findOne (
			"countWithAlertAndFatal ()",
			Long.class,

			createCriteria (
				ExceptionLogRec.class,
				"_exceptionLog")

			.add (
				Restrictions.eq (
					"_exceptionLog.alert",
					true))

			.add (
				Restrictions.eq (
					"_exceptionLog.fatal",
					true))

			.setProjection (
				Projections.rowCount ())

		);

	}

	@Override
	public
	List <Long> searchIds (
			ExceptionLogSearch search) {

		Criteria criteria =
			createCriteria (
				ExceptionLogRec.class);

		if (search.alert () != null) {

			criteria.add (
				Restrictions.eq (
					"alert",
					search.alert ()));

		}

		if (search.fatal () != null) {

			criteria.add (
				Restrictions.eq (
					"fatal",
					search.fatal ()));

		}

		if (search.typeId () != null) {

			ExceptionLogTypeRec type =
				get (
					ExceptionLogTypeRec.class,
					search.typeId ());

			criteria.add (
				Restrictions.eq (
					"type",
					type));

		}

		if (search.userId () != null) {

			UserRec user =
				get (
					UserRec.class,
					search.userId ());

			criteria.add (
				Restrictions.eq (
					"user",
					user));

		}

		if (search.order () != null) {

			switch (search.order ()) {

			case timestampDesc:

				criteria.addOrder (
					Order.desc (
						"timestamp"));

				break;

			default:

				throw new IllegalArgumentException ();

			}

		}

		if (search.maxResults () != null) {

			criteria.setMaxResults (
				toJavaIntegerRequired (
					search.maxResults ()));

		}

		criteria.setProjection (
			Projections.id ());

		return findMany (
			"searchIds (search)",
			Long.class,
			criteria);

	}

	@Override
	public
	List <ExceptionLogRec> findOldLimit (
			@NonNull Instant cutoffTime,
			@NonNull Long maxResults) {

		return findMany (
			"findOldLimit (cutoffTime, maxResults)",
			ExceptionLogRec.class,

			createCriteria (
				ExceptionLogRec.class,
				"_exceptionLog")

			.add (
				Restrictions.lt (
					"_exceptionLog.timestamp",
					cutoffTime))

			.addOrder (
				Order.asc (
					"_exceptionLog.timestamp"))

			.setMaxResults (
				toJavaIntegerRequired (
					maxResults))

		);

	}

}
