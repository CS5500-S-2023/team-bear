package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.Listing;
import edu.northeastern.cs5500.starterbot.model.ListingFields;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
@SuppressWarnings("null")
class ListingControllerTest {
    static final String USER_ID = "631666734125987209";
    static final String GUILD_ID = "294764645159495548";
    static final String TITLE = "test";
    ListingFields LISTING_FIELDS;
    Listing TEST_LISTING;
    ListingController listingController;

    @BeforeAll
    void createListing() {
        var listingFields =
                ListingFields.builder()
                        .cost("123")
                        .description("test description")
                        .shippingIncluded(false)
                        .condition("Good")
                        .datePosted("test date")
                        .build();

        LISTING_FIELDS = listingFields;
        Objects.requireNonNull(listingFields);
        TEST_LISTING =
                Listing.builder()
                        .id(new ObjectId())
                        .messageId(123455677)
                        .discordUserId(USER_ID)
                        .guildId(GUILD_ID)
                        .title(TITLE)
                        .url("test url")
                        .images(new ArrayList<>(Arrays.asList("Test url", "test")))
                        .fields(listingFields)
                        .build();
    }

    @BeforeEach
    void getListingController() {
        listingController = new ListingController(new InMemoryRepository<>());
    }

    @Test
    void testAddListingActuallyAddsListing() {
        // setup
        Collection<Listing> testCollection = Arrays.asList(TEST_LISTING);

        // precondition
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID))
                .isNotEqualTo(testCollection);

        // mutation
        listingController.addListing(TEST_LISTING);

        // postcondition
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID))
                .isEqualTo(testCollection);
    }

    @Test
    void testDeleteListingByMemberIdActuallyDeletesListing() {
        // precondition
        listingController.addListing(TEST_LISTING);
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isNotEmpty();

        // mutation
        assertThat(listingController.deleteListingsForUser(USER_ID, GUILD_ID)).isTrue();

        // post
        assertThat(listingController.deleteListingsForUser(USER_ID, GUILD_ID)).isFalse();
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isEmpty();
    }

    @Test
    void testDeleteListingByIdActuallyDeletesListing() {
        // precondition
        listingController.addListing(TEST_LISTING);
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isNotEmpty();

        // mutation
        assertThat(listingController.deleteListingById(TEST_LISTING.getId(), USER_ID)).isTrue();

        // post
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isEmpty();
        assertThat(listingController.deleteListingById(TEST_LISTING.getId(), USER_ID)).isFalse();

        ListingFields listingFields =
                ListingFields.builder()
                        .cost("123")
                        .description("test description")
                        .shippingIncluded(false)
                        .condition("Good")
                        .datePosted("test date")
                        .build();

        Listing listingNotMatch =
                Listing.builder()
                        .id(new ObjectId())
                        .messageId(123455677)
                        .discordUserId("different user")
                        .guildId(GUILD_ID)
                        .title(TITLE)
                        .url("url")
                        .images(new ArrayList<>(Arrays.asList("url", "test")))
                        .fields(listingFields)
                        .build();

        listingController.addListing(listingNotMatch);

        assertThat(listingController.deleteListingById(listingNotMatch.getId(), USER_ID)).isFalse();
    }

    @Test
    void testCountListingsByMemberId() {
        // precondition
        assertThat(listingController.countListingsByMemberId(USER_ID, GUILD_ID)).isEqualTo(0);

        // mutation
        listingController.addListing(TEST_LISTING);

        // post
        assertThat(listingController.countListingsByMemberId(USER_ID, GUILD_ID)).isEqualTo(1);
    }

    @Test
    void getListingsWithKeyword() {
        // precondition
        assertThat(listingController.getListingsWithKeyword(TITLE, GUILD_ID)).isNotNull();

        // mutation
        listingController.addListing(TEST_LISTING);
        Collection<Listing> testCollection = Arrays.asList(TEST_LISTING);

        // post
        assertThat(listingController.getListingsWithKeyword(TITLE, GUILD_ID))
                .isEqualTo(testCollection);
    }

    @Test
    void testGetListingsByMemberId() {
        // setup
        Collection<Listing> testCollection = Arrays.asList(TEST_LISTING);

        // precondition
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isNotNull();
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isEmpty();

        // mutation
        listingController.addListing(TEST_LISTING);

        // post
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID))
                .isEqualTo(testCollection);
        ;
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isNotEmpty();
    }

    @Test
    void testGetListingById() {
        // precondition
        assertThat(listingController.getListingById(TEST_LISTING.getId())).isNull();

        // mutation
        listingController.addListing(TEST_LISTING);

        // post
        assertThat(listingController.getListingById(TEST_LISTING.getId())).isEqualTo(TEST_LISTING);
        ;
        assertThat(listingController.getListingById(TEST_LISTING.getId())).isNotNull();
    }

    @Test
    void testGetAllListingsInGuild() {
        Collection<Listing> testCollection = Arrays.asList(TEST_LISTING);

        // precondition
        assertThat(listingController.getAllListingsInGuild(GUILD_ID)).isNotNull();
        assertThat(listingController.getAllListingsInGuild(GUILD_ID)).isEmpty();

        // mutation
        listingController.addListing(TEST_LISTING);

        // post
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID))
                .isEqualTo(testCollection);
        ;
        assertThat(listingController.getListingsByMemberId(USER_ID, GUILD_ID)).isNotEmpty();
    }
}