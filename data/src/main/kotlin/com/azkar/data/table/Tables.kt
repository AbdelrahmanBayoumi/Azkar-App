package com.azkar.data.table

import com.azkar.domain.model.CollectionKind
import com.azkar.domain.model.HadithGrade
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.charLength
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.javatime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

// The single source of truth for this application's schema.
//
// Exposed both describes and creates the tables: `migrateToLatestSchema` asks `MigrationUtils` for the DDL
// that brings a database in line with these objects and runs it. There is no SQL script anywhere in
// the build, so a column, an index or a constraint cannot exist in one place and be missing from the
// other. That function is schema and nothing else — the dataset is loaded once by `LibraryBootstrapper`
// on the application's first launch, not on every start, because a loader that ran every time would
// need to recognise rows it had written before, and recognising them means carrying an identity on
// them that the user's own edits could invalidate.
//
// Every table below is an `IdTable` — `LongIdTable`, `CompositeIdTable`, or a hand-rolled `IdTable`
// where the key is a foreign key the application assigns itself. That is not decoration: an entity
// class cannot be built over a plain `Table`, so a table that is not an identity table has no DAO
// entity, and every statement about it would stay hand-written row mapping. The price is that a key
// column is typed `Column<EntityID<T>>` rather than `Column<T>`, so reading one is `.value`. Writing
// one is not: `eq`, `inList` and `notInList` are declared over that type already and take the raw
// value, so a predicate reads exactly as it did before the switch. See `com.azkar.data.entity` for
// the classes these columns back.
//
// Every table extends one of the two base classes below, so the audit columns are declared once for
// the whole schema rather than once per table. Every timestamp column is
// `TIMESTAMP WITH TIME ZONE` defaulting to the current instant on the database side. Exposed maps
// that through `timestampWithTimeZone()` to OffsetDateTime, while the domain model speaks
// java.time.Instant, so the mapping layer converts between the two.
//
// Nothing here is maintained by the database beyond that default: a column with a default but no
// trigger still records when the row was first written, not when it was last changed. `updated_at`
// in particular is bumped by the application on every write, which is why the writers below set
// it explicitly rather than trusting the schema.
//
// Indexes and check constraints are declared in `init` blocks because the DSL calls are
// expressions, and an expression may not stand on its own in a class body. Each block therefore
// sits below the columns it names, so they are already initialized when it runs. A base class runs
// its `init` before its subclass bodies, so the same holds for a subclass declaring indexes over
// columns it inherited.

/** All tables of the schema, in the order they are handed to the migration utility. */
val ALL_TABLES: Set<Table> =
    setOf(
        TagTable,
        TagTranslationTable,
        RemembranceTable,
        RemembranceTranslationTable,
        ExplanationTranslationTable,
        RemembranceTagTable,
        CollectionTable,
        CollectionTranslationTable,
        CollectionMemberTable,
        CollectionTagTable,
        RecitationProgressTable,
    )

/**
 * The base of every single-key table in the schema, carrying the two audit columns.
 *
 * `IdTable<Long>` rather than `LongIdTable`, so a table is free to name its key column itself
 * instead of taking the `id`/`primaryKey` pair that `LongIdTable` declares final and
 * unconditionally auto-incrementing.
 */
abstract class TimestampedIdTable(
    name: String,
) : IdTable<Long>(name) {
    val createdAt =
        timestampWithTimeZone(CREATED_AT)
            .defaultExpression(CurrentTimestampWithTimeZone)
    val updatedAt =
        timestampWithTimeZone(UPDATED_AT)
            .defaultExpression(CurrentTimestampWithTimeZone)
}

/**
 * The base of every table in the schema whose key is a set of columns rather than one.
 *
 * A join table or a per-pair record — membership, labels, links, progress — has no surrogate key to
 * offer, so its identity is the pair itself, and `CompositeIdTable` is the only shape that can say
 * so and still be an entity table.
 *
 * Open rather than abstract, because `CompositeIdTable` already supplies the one member `IdTable`
 * leaves abstract — the placeholder `id` column — and each subclass declares its own `primaryKey`
 * over the pair it is built from. There is nothing left here for a subclass to be forced to fill in,
 * so `abstract` would be a claim about the hierarchy that the compiler can see is not true.
 */
open class TimestampedCompositeIdTable(
    name: String,
) : CompositeIdTable(name) {
    val createdAt =
        timestampWithTimeZone(CREATED_AT)
            .defaultExpression(CurrentTimestampWithTimeZone)
    val updatedAt =
        timestampWithTimeZone(UPDATED_AT)
            .defaultExpression(CurrentTimestampWithTimeZone)
}

// `tag` — the one label the user can attach to a remembrance.
object TagTable : TimestampedIdTable("tag") {
    override val id = long("id").autoIncrement().entityId()

    override val primaryKey = PrimaryKey(id)

    /**
     * Stored folded to lower case, and the only way to write it: [com.azkar.domain.model.impl.TagImpl] folds in its
     * constructor, so a `Tag` in memory is already lower case by the time a row is written.
     *
     * That is what lets the uniqueness check below be a plain unique index. A unique index on a
     * text column is case-sensitive — `'Morning'` and `'morning'` are different values to it — so
     * folding at the boundary is not a display convenience, it is the invariant the index relies
     * on. See `TagImpl` and `CollectionPresenter` for the two ends of it.
     */
    val name = varchar("name", MAX_NAME_LENGTH)

    /** BCP-47 language tag for the canonical label; direction and script do not identify a language. */
    val nameLocaleCode = varchar("name_locale_code", 64).default("en")

    /**
     * When this tag was taken out of circulation, or `null` while it is still in use.
     *
     * A tombstone rather than a delete, and the whole reason this table has one column that the other
     * nine tables do not. A tag is shared: the row is a single fact that a dozen collections and a
     * hundred remembrances each hold an edge to, and the `collection_tag` / `remembrance_tag` rows are
     * what make that sharing cheap. Removing the tag outright therefore cannot mean "forget it" — it
     * would mean silently rewriting the labels of every collection carrying it, which is a larger
     * edit than the person who clicked Delete was asking to make.
     *
     * So the row stays, the edges stay, and only the reading changes: every read of `tag` outside
     * `TagService.restore` asks for `deleted_at IS NULL`. That makes the deletion reversible for free
     * — clearing this column brings the tag back to the exact shelves it left, because the edges that
     * were never touched are still pointing at the same id.
     *
     * Null rather than a flag because a flag has a fourth state to get wrong. With a boolean,
     * `false` would have to mean both "deleted, and the date is unknown" and "not deleted", and
     * [createdAt] next to it is the evidence that the date is always written.
     */
    val deletedAt = timestampWithTimeZone(DELETED_AT).nullable()

    init {
        // Serves `findByNameIgnoreCase` directly, since the name it is handed is folded too.
        index("uq_tag__name", isUnique = true, name)
    }
}

// `remembrance` — the core zikr record.
object RemembranceTable : TimestampedIdTable("remembrance") {
    override val id = long("id").autoIncrement().entityId()

    override val primaryKey = PrimaryKey(id)

    /** Named `sourceText` because [org.jetbrains.exposed.v1.core.ColumnSet] already owns a `source` member. */
    val sourceText = varchar("source", MAX_NAME_LENGTH).nullable()

    // Stored as the enum constant's name, which keeps the column portable plain text rather than
    // tying the schema to a vendor-specific ENUM type.
    val grade =
        enumerationByName<HadithGrade>("grade", MAX_GRADE_LENGTH)
            .default(HadithGrade.UNSPECIFIED)

    // Whether the user has starred this remembrance. A plain column rather than the surrogate
    // `favorite` row this replaces: the row carried nothing but its own `created_at`, and nothing
    // ever read that stamp — the star carries no date anywhere in the domain model or the UI.
    // Modelling the star as a row meant an extra insert, an extra delete and a nullable foreign
    // key whose only two states were "points at a row" and "null".
    val isFavorite = bool("is_favorite").default(false)

    init {
        // The starred rail and the favourites filter both scan this column, and it is the one
        // predicate every starred row has to satisfy.
        index("idx_remembrance__is_favorite", isUnique = false, isFavorite)
    }
}

// `remembrance_translation` and `explanation_translation` — a remembrance's own words, and the
// commentary on them, each rendered in one locale. `tag_translation` — a label's own wording, in
// one locale — is the same shape against a different parent.
//
// All three hold the same thing about the same kind of parent and differ only in what they hang
// off, so they are one table three times. Everything below is therefore declared once, against
// [LocalizedTextTable], which also gives the loader and the writer a single type to be handed
// instead of a handwritten view over five columns.
//
// `locale_code` holds a BCP-47 language tag (`ar`, `en`, …); the mappers convert with
// Locale.forLanguageTag / Locale.toLanguageTag.
open class LocalizedTextTable(
    name: String,
    // Index and check names have to be unique across the schema, so the subclass supplies the
    // stem its four names are built from. Passing it in rather than deriving it from `name` keeps
    // `uq_rt__…` and `uq_et__…` the names they have always been, which is what an existing
    // database and its tests already refer to.
    indexPrefix: String,
    // The foreign key column and the table it points at, supplied together because the two cannot
    // come apart: a column named after one parent and constrained to another is a table that lies.
    //
    // Named rather than inferred so that `remembrance_id` stays spelled the way every installed
    // database already spells it. Deriving the column name from the owner table would work for the
    // two tables that exist today and quietly produce a rename for the third, and the migration
    // utility only ever emits CREATE — a renamed column would be an ADD on a table that already has
    // the old one, so the constraint would land twice rather than move.
    ownerColumnName: String,
    owner: IdTable<Long>,
) : TimestampedIdTable(name) {
    override val id = long("id").autoIncrement().entityId()

    override val primaryKey = PrimaryKey(id)

    // The stem the three index names are built from, taken off the column name rather than being
    // passed again: `remembrance_id` yields `remembrance`, which is what `uq_rt__remembrance_locale`
    // and `idx_rt__by_remembrance` have always been called.
    private val ownerStem = ownerColumnName.removeSuffix("_id")

    val ownerId =
        long(ownerColumnName)
            .references(owner.id, onDelete = ReferenceOption.CASCADE)
    val localeCode = varchar("locale_code", MAX_LOCALE_LENGTH)
    val text = varchar("text", MAX_TEXT_LENGTH)

    init {
        // One text per locale, and the lookups that read them back are all by one of the two.
        uniqueIndex("uq_${indexPrefix}__${ownerStem}_locale", ownerId, localeCode)
        index("idx_${indexPrefix}__by_$ownerStem", isUnique = false, ownerId)
        index("idx_${indexPrefix}__by_locale", isUnique = false, localeCode)

        check("ck_${indexPrefix}__text_not_blank") { text.charLength() greater 0 }
    }
}

@Suppress("ExposedReference")
object
RemembranceTranslationTable :
    LocalizedTextTable("remembrance_translation", "rt", "remembrance_id", RemembranceTable)

@Suppress("ExposedReference")
object
ExplanationTranslationTable :
    LocalizedTextTable("explanation_translation", "et", "remembrance_id", RemembranceTable)

// `tag_translation` — a label's wording in one locale.
//
// Same shape as a remembrance's translations and for the same reason: the shipped tags are named in
// English because that is what the dataset ships, and a name that cannot be rendered in the
// language the app is in looks unfinished everywhere it appears. The folded `tag.name` stays the
// canonical value — the unique index is built on it and the avatar colour is hashed from it — so
// this table is a display layer over it rather than a replacement for it.
@Suppress("ExposedReference")
object TagTranslationTable : LocalizedTextTable("tag_translation", "tt", "tag_id", TagTable)

// `remembrance_tag` — composite-key join table between remembrances and tags.
object RemembranceTagTable : TimestampedCompositeIdTable("remembrance_tag") {
    // `entityId()` is what makes these the identity rather than two more foreign keys: it
    // registers the column with the table's key, and `references(...)` has already attached the
    // cascade. `references` is the plain overload because the target is an `IdTable` column, so it
    // returns the `Column<Long>` that `entityId()` then retypes.
    val remembranceId =
        long("remembrance_id")
            .references(RemembranceTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()

    val tagId =
        long("tag_id")
            .references(TagTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()

    // The pair is the identity, so no surrogate key is needed.
    override val primaryKey = PrimaryKey(remembranceId, tagId)

    init {
        // The two indexes serve the "which tags does this remembrance carry" and "which
        // remembrances carry this tag" questions independently.
        index("idx_remembrance_tag__by_remembrance", isUnique = false, remembranceId)
        index("idx_remembrance_tag__by_tag", isUnique = false, tagId)
    }
}

// `collection` — a shelf of remembrances. Every collection is the user's: there is no catalogue key
// on this row, so nothing here distinguishes a shelf that shipped from one they built, and nothing
// here decides what is allowed to change it.
//
// `kind` therefore records only provenance — what the card's badge says and which shelf-scope chips
// offer it — and never membership. A collection's members are rows of `collection_remembrance`
// whichever kind it is, which is why the loader has a single membership query rather than two
// sources to reconcile.
//
// The two pins answer different questions and stay separate. `pinned_at` is top of the shelf, which
// is where the user finds a shelf among everything else; `home_pinned_at` is membership of the few
// they reach for daily, and the Home rail orders by it. One flag for both would force the hub's
// Pinned chip to disagree with what Home shows.
//
// `cover_image_path` names a file inside the application's own covers directory rather than holding
// a URL or a caller-supplied path. Copying the picture the user picked into storage the app owns is
// what makes "they deleted the image" an event this app can see: it can fall back to the generated
// wash, sweep the orphan and stop pointing at something that is gone.
object CollectionTable : TimestampedIdTable("collection") {
    override val id = long("id").autoIncrement().entityId()

    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", MAX_NAME_LENGTH)

    /** BCP-47 language tag for the canonical name, stored separately from its text. */
    val nameLocaleCode = varchar("name_locale_code", 64).default("en")

    val description = varchar("description", MAX_TEXT_LENGTH).nullable()

    val kind = enumerationByName<CollectionKind>("kind", MAX_ENUM_LENGTH)

    // A glyph key the user chose, or null to derive one from the tags and the cover. Storing null
    // rather than a derived value is the point: the derivation changes when the tags change, and a
    // stored answer could not tell a deliberate choice from a stale one.
    val iconKey = varchar("icon_key", MAX_NAME_LENGTH).nullable()

    val coverImagePath = varchar("cover_image_path", MAX_NAME_LENGTH).nullable()

    // A stamp rather than a flag, matching `home_pinned_at` below, and for the same reason: the
    // shelf orders its pinned cards by recency and a boolean cannot order anything.
    val pinnedAt = timestampWithTimeZone("pinned_at").nullable()

    // Nullable, and deliberately without a default, so a collection the user has never opened reads
    // as null rather than as "opened when the row was created". A default here would make the
    // recently-opened ordering report every collection as visited at seed time.
    val lastOpenedAt = timestampWithTimeZone("last_opened_at").nullable()

    // Nullable and without a default, for the same reason as `last_opened_at`: a collection the
    // user never put on the Home screen reads as null rather than as "added at seed time", which
    // would put every shipped collection on the rail the first time the app is opened. The stamp
    // also orders the rail, most recently pinned first, without a second sort key.
    val homePinnedAt = timestampWithTimeZone("home_pinned_at").nullable()

    // Whether the user has marked this collection as one they come back to. A flag rather than a
    // stamp, because unlike the Home rail nothing orders favourites yet and a favourite that sorted
    // by when it was starred would be a second, silent ordering to explain. Defaulting to false is
    // what makes the column safe to add to a table that already has rows: every existing collection
    // reads as not favourited, which is the truth about all of them.
    val isFavorite = bool("is_favorite").default(false)

    // Where the collection sits in the user's own order of their library, or null when it has none.
    // Nullable and without a default, for the same reason as `last_opened_at`: a collection the user
    // has never placed reads as null rather than as "placed at creation time", which would put every
    // shipped collection in the order the moment the app was opened. Nulls sort last, so the ones the
    // user did arrange come first and the rest keep a stable order among themselves.
    val position = integer("position").nullable()

    init {
        // The hub's default sort is "recently updated", and a user's own shelves are the rows
        // that move; either way the scan starts here.
        index("idx_collection__by_kind", isUnique = false, kind)
    }
}

// `collection_translation` — a shelf's name and description in one locale.
//
// Not a [LocalizedTextTable] because a collection has two pieces of prose rather than one, and two
// tables joined only by locale would make every read a self-join to put back together what one row
// already holds. The reminder is the reason the description is nullable: a translation is added
// because somebody asked for the name in another language, and asking for the name is not asking
// for prose, so a name with nothing behind it is an ordinary row rather than a half-finished one.
//
// `collection.name` and `collection.description` remain the canonical values and are never emptied
// to make room for these — the same reason `tag.name` stays folded: the unique index, the
// search, the sort and the rename check are all built on them.
object CollectionTranslationTable : TimestampedIdTable("collection_translation") {
    override val id = long("id").autoIncrement().entityId()

    override val primaryKey = PrimaryKey(id)

    val collectionId =
        long("collection_id")
            .references(CollectionTable.id, onDelete = ReferenceOption.CASCADE)
    val localeCode = varchar("locale_code", MAX_LOCALE_LENGTH)
    val name = varchar("name", MAX_NAME_LENGTH)
    val description = varchar("description", MAX_TEXT_LENGTH).nullable()

    init {
        // One pair per locale, and the reader looks every locale up by this pair.
        index("uq_ct__collection_locale", isUnique = true, collectionId, localeCode)
        index("idx_ct__by_collection", isUnique = false, collectionId)
        index("idx_ct__by_locale", isUnique = false, localeCode)

        // The name is what the shelf card draws, so a blank one would render as an empty row. The
        // description is free to be absent; the name is not.
        check("ck_ct__name_not_blank") { name.charLength() greater 0 }
    }
}

// `collection_remembrance` — membership, in display order. Every collection has rows here; there
// is no second source of members to reconcile against.
//
// `position` is a dense 0-based rank kept stable by the repository; it is deliberately not unique,
// because removing from the middle would otherwise force a rewrite of every following row before a
// single delete could commit.
//
// `created_at` is inherited, and is when the member joined this collection — the one thing
// `position` cannot say, because a row that survives two inserts above it keeps its rank.
object CollectionMemberTable : TimestampedCompositeIdTable("collection_remembrance") {
    val collectionId =
        long("collection_id")
            .references(CollectionTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()
    val remembranceId =
        long("remembrance_id")
            .references(RemembranceTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()
    val position = integer("position").default(0)

    // The pair is the identity, so no surrogate key is needed.
    override val primaryKey = PrimaryKey(collectionId, remembranceId)

    init {
        // Reading a shelf is always "this collection, in this order", so the composite index
        // serves both the lookup and the sort from one structure.
        index("idx_collection_member__by_order", isUnique = false, collectionId, position)
        index("idx_collection_member__by_remembrance", isUnique = false, remembranceId)
    }
}

// `collection_tag` — the labels a user has put on a collection, drawn from the same `tag` rows the
// remembrances use so one vocabulary covers both.
//
// Mirrors `remembrance_tag` exactly, and inherits no ordering column: a tag has no position in a
// collection because a collection's members are the ordered thing, not its labels.
object CollectionTagTable : TimestampedCompositeIdTable("collection_tag") {
    val collectionId =
        long("collection_id")
            .references(CollectionTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()

    val tagId =
        long("tag_id")
            .references(TagTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()

    // The pair is the identity, so no surrogate key is needed.
    override val primaryKey = PrimaryKey(collectionId, tagId)

    init {
        // The two indexes serve the "which tags does this collection carry" and "which collections
        // carry this tag" questions independently.
        index("idx_collection_tag__by_collection", isUnique = false, collectionId)
        index("idx_collection_tag__by_tag", isUnique = false, tagId)
    }
}

// `recitation_progress` — how far through a collection the user has got.
//
// Keyed on the collection as well as the remembrance, because the same passage can sit in several
// collections at several positions and one shared counter would let finishing one silently advance
// the rest. Both keys CASCADE: deleting a collection deletes its progress with it, and deleting a
// remembrance deletes the progress that referred to it, so a row can never outlive what it counts.
object RecitationProgressTable : TimestampedCompositeIdTable("recitation_progress") {
    val collectionId =
        long("collection_id")
            .references(CollectionTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()
    val remembranceId =
        long("remembrance_id")
            .references(RemembranceTable.id, onDelete = ReferenceOption.CASCADE)
            .entityId()

    // Defaulted rather than nullable-with-no-default: a row only exists once something has been
    // counted, so zero is a real state and an absent column is not one.
    val repeatCount = integer("repeat_count").default(0)

    // Nullable so "never recited" is distinguishable from "recited before the column existed",
    // which matters to a reminder deciding whether today's session is already done.
    val lastRecitedAt = timestampWithTimeZone("last_recited_at").nullable()

    override val primaryKey = PrimaryKey(collectionId, remembranceId)

    init {
        // The primary key already serves "this collection's progress" as a prefix scan. This one
        // serves the opposite direction, which the reminders need: what has this passage been
        // counted for, across every collection it belongs to.
        index("idx_recitation_progress__by_remembrance", isUnique = false, remembranceId)
    }
}

private const val CREATED_AT = "created_at"
private const val UPDATED_AT = "updated_at"
private const val DELETED_AT = "deleted_at"
private const val MAX_NAME_LENGTH = 255
private const val MAX_GRADE_LENGTH = 32
private const val MAX_ENUM_LENGTH = 32
private const val MAX_LOCALE_LENGTH = 35
private const val MAX_TEXT_LENGTH = 4_096
