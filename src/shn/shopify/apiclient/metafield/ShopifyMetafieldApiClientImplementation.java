package shn.shopify.apiclient.metafield;

import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.etc.DebugUtils.debugFormat;
import static wbs.utils.etc.Misc.lessThan;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import wbs.framework.apiclient.GenericHttpSender;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import shn.shopify.apiclient.ShopifyApiClientCredentials;
import shn.shopify.apiclient.ShopifyApiRequest;
import shn.shopify.apiclient.ShopifyApiResponse;

@SingletonComponent ("shopifyMetafieldApiClient")
public
class ShopifyMetafieldApiClientImplementation
	implements ShopifyMetafieldApiClient {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("shopifyHttpSender")
	ComponentProvider <GenericHttpSender <
		ShopifyApiRequest,
		ShopifyApiResponse
	>> shopifyHttpSenderProvider;

	// public implementation

	@Override
	public
	List <ShopifyMetafieldResponse> listAll (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ShopifyApiClientCredentials credentials) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"listAll");

		) {

			ImmutableList.Builder <ShopifyMetafieldResponse> builder =
				ImmutableList.builder ();

			for (
				long page = 0l;
				true;
				page ++
			) {

				ShopifyMetafieldListResponse response =
					genericCastUnchecked (
						shopifyHttpSenderProvider.provide (
							taskLogger)

					.allInOne (
						taskLogger,
						new ShopifyMetafieldListRequest ()

						.httpCredentials (
							credentials)

						.limit (
							250l)

						.page (
							page)

					)

				);

				builder.addAll (
					response.metafields ());

				if (
					lessThan (
						collectionSize (
							response.metafields ()),
						250l)
				) {
					break;
				}

			}

			return builder.build ();

		}

	}

	@Override
	public
	List <ShopifyMetafieldResponse> listByNamespaceAndOwnerResource (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ShopifyApiClientCredentials credentials,
			@NonNull String namespace,
			@NonNull String ownerResource) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"listAll");

		) {

			ImmutableList.Builder <ShopifyMetafieldResponse> builder =
				ImmutableList.builder ();

			for (
				long page = 0l;
				true;
				page ++
			) {

				ShopifyMetafieldListResponse response =
					genericCastUnchecked (
						shopifyHttpSenderProvider.provide (
							taskLogger)

					.allInOne (
						taskLogger,
						new ShopifyMetafieldListRequest ()

						.httpCredentials (
							credentials)

						.limit (
							250l)

						.page (
							page)

						.namespace (
							namespace)

						.metafieldOwnerResource (
							ownerResource)

					)

				);

debugFormat (
	"GOT %s METAFIELDS",
	integerToDecimalString (
		collectionSize (
			response.metafields ())));

				builder.addAll (
					response.metafields ());

				if (
					lessThan (
						collectionSize (
							response.metafields ()),
						250l)
				) {
					break;
				}

			}

			return builder.build ();

		}

	}

	@Override
	public
	ShopifyMetafieldResponse create (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ShopifyApiClientCredentials credentials,
			@NonNull ShopifyMetafieldRequest request) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"create");

		) {

			ShopifyMetafieldCreateResponse response =
				genericCastUnchecked (
					shopifyHttpSenderProvider.provide (
						taskLogger)

				.allInOne (
					taskLogger,
					new ShopifyMetafieldCreateRequest ()

					.httpCredentials (
						credentials)

					.metafield (
						request)

				)

			);

			return response.metafield ();

		}

	}

	@Override
	public
	ShopifyMetafieldResponse update (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ShopifyApiClientCredentials credentials,
			@NonNull ShopifyMetafieldRequest metafield) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"update");

		) {

			ShopifyMetafieldUpdateResponse response =
				genericCastUnchecked (
					shopifyHttpSenderProvider.provide (
						taskLogger)

				.allInOne (
					taskLogger,
					new ShopifyMetafieldUpdateRequest ()

					.httpCredentials (
						credentials)

					.metafield (
						metafield)

				)

			);

			return response.metafield ();

		}

	}

	@Override
	public
	void remove (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ShopifyApiClientCredentials credentials,
			@NonNull Long id) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"remove");

		) {

			shopifyHttpSenderProvider.provide (
				taskLogger)

				.allInOne (
					taskLogger,
					new ShopifyMetafieldRemoveRequest ()

					.httpCredentials (
						credentials)

					.id (
						id)

				)

			;

		}

	}

}