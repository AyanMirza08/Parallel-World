package com.parallel.app.domain

/**
 * Deterministic offline generation. It returns the same structured record for the same inputs,
 * and can be replaced by an API-backed implementation without changing the saved-world/UI contract.
 */
object LocalWorldGenerator {
    private const val DEFAULT_CURRENT_YEAR = 2026

    fun generate(location: String, country: String, year: String?, premise: String): ParallelWorld {
        val place = location.trim().ifBlank { "Unknown Place" }
        val nation = country.trim().ifBlank { "Unknown Country" }
        val whatIf = premise.trim()
        val currentYear = year?.toIntOrNull()?.takeIf { it in 1..9999 } ?: DEFAULT_CURRENT_YEAR
        val seedText = listOf(place, nation, currentYear.toString(), whatIf).joinToString("|") { it.lowercase() }
        val seed = seedText.hashCode().toLong() and 0xffffffffL
        val theme = classify(whatIf, seed)
        val denied = rejectsDefault(whatIf)
        val pattern = pattern(theme, denied)
        val variant = (seed % pattern.names.size).toInt()
        val span = minOf(36 + (seed % 105).toInt(), (currentYear - 1).coerceAtLeast(0))
        val divergenceYear = (currentYear - span).coerceAtLeast(1)
        val population = populationFor(theme, denied, seed)
        val divergence = whatIf.ifBlank { pattern.defaultDivergence.replace("{place}", place) }
        val summary = pattern.summary.replace("{place}", place)
            .replace("{country}", nation)
            .replace("{population}", formatPopulation(population))

        val events = eventYears(divergenceYear, currentYear).mapIndexed { index, eventYear ->
            WorldEvent(
                year = eventYear,
                title = pattern.eventTitles[index],
                description = when (index) {
                    0 -> "The timeline changes in $place: $divergence"
                    1 -> pattern.middleEvent.replace("{place}", place)
                    else -> pattern.lateEvent.replace("{place}", place).replace("{country}", nation)
                },
                historicalSignificance = historicalSignificance(theme, denied, index, place)
            )
        }

        return ParallelWorld(
            id = "world-" + seed.toString(16),
            name = place + ": " + pattern.names[variant],
            location = place,
            country = nation,
            alternateTimelineSummary = summary,
            divergencePoint = divergence,
            divergenceYear = divergenceYear,
            currentYear = currentYear,
            population = population,
            government = WorldGovernment(pattern.governmentSystem, pattern.governmentDescription.replace("{place}", place)),
            economy = WorldEconomy(pattern.economyDescription.replace("{place}", place), pattern.sectors),
            culture = WorldCulture(pattern.cultureDescription.replace("{place}", place), pattern.culturalTraits),
            technology = WorldTechnology(pattern.technologyLevel, pattern.technologyDescription.replace("{place}", place)),
            majorHistoricalEvents = events,
            importantLocations = pattern.locations.mapIndexed { index, (name, detail) ->
                WorldLocation(
                    name = name.replace("{place}", place),
                    description = detail.replace("{place}", place),
                    whyItMatters = locationImportance(theme, denied, index, place)
                )
            },
            importantCharacters = charactersFor(theme, denied, variant, place, nation),
            strangeAnomalies = anomaliesFor(theme, denied, place, pattern.anomalies)
        )
    }

    private enum class Theme { MOBILITY, GOVERNANCE, CLIMATE, COMPUTING, INDUSTRY, CULTURE, CIVIC }

    private data class Pattern(
        val names: List<String>,
        val summary: String,
        val defaultDivergence: String,
        val governmentSystem: String,
        val governmentDescription: String,
        val economyDescription: String,
        val sectors: List<String>,
        val cultureDescription: String,
        val culturalTraits: List<String>,
        val technologyLevel: String,
        val technologyDescription: String,
        val eventTitles: List<String>,
        val middleEvent: String,
        val lateEvent: String,
        val locations: List<Pair<String, String>>,
        val anomalies: List<Pair<String, String>>
    )

    private fun classify(premise: String, seed: Long): Theme {
        val words = premise.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter(String::isNotBlank).toSet()
        val groups = linkedMapOf(
            Theme.MOBILITY to setOf("rail", "railway", "train", "trains", "metro", "subway", "transit", "traffic", "commute", "highway", "ferry", "airport", "hub", "station", "transport"),
            Theme.GOVERNANCE to setOf("government", "democracy", "democratic", "monarchy", "monarch", "king", "queen", "empire", "imperial", "republic", "election", "revolution", "independence", "autonomy", "colony", "colonial"),
            Theme.CLIMATE to setOf("climate", "flood", "floods", "flooded", "river", "drought", "heat", "storm", "earthquake", "water", "sea", "ocean", "coast"),
            Theme.COMPUTING to setOf("computer", "computers", "internet", "digital", "artificial", "intelligence", "ai", "electricity", "energy", "automation", "robot", "robots", "network"),
            Theme.INDUSTRY to setOf("economy", "economic", "industry", "industrial", "factory", "factories", "manufacturing", "trade", "trading", "port", "mining", "agriculture", "farming", "oil", "commerce"),
            Theme.CULTURE to setOf("language", "culture", "cultural", "religion", "art", "arts", "music", "migration", "immigrant", "identity", "community")
        )
        val scores = groups.mapNotNull { (theme, terms) ->
            words.count { it in terms }.takeIf { it > 0 }?.let { theme to it }
        }
        val highest = scores.maxOfOrNull { it.second } ?: return Theme.CIVIC
        val tied = scores.filter { it.second == highest }.map { it.first }
        return tied[(seed % tied.size).toInt()]
    }

    private fun rejectsDefault(premise: String): Boolean {
        val text = premise.lowercase()
        return listOf("never", "no longer", "without", "failed to", "did not", "didn't", "avoided", "rejected", "stopped", "not become")
            .any(text::contains)
    }

    private fun pattern(theme: Theme, denied: Boolean): Pattern {
        val selected = when (theme) {
            Theme.MOBILITY -> mobilityPattern(denied)
            Theme.GOVERNANCE -> governancePattern(denied)
            Theme.CLIMATE -> climatePattern(denied)
            Theme.COMPUTING -> computingPattern(denied)
            Theme.INDUSTRY -> industryPattern(denied)
            Theme.CULTURE -> culturePattern(denied)
            Theme.CIVIC -> civicPattern
        }
        return if (theme == Theme.MOBILITY) selected
        else selected.copy(anomalies = anomalySeeds(theme, denied))
    }

    private fun mobilityPattern(denied: Boolean) = if (denied) pattern(
        names = listOf("The District Routes Compact", "Many Small Stations", "The Walkable City Accord"),
        summary = "After it never became a single dominant rail hub, {place} in {country} grew as a network of district centers. Its {population} residents use short local lines, buses, walking streets, and regional links rather than one vast interchange.",
        divergence = "The plan to concentrate the region's rail traffic in {place} is rejected in favor of local centers.",
        system = "Metropolitan district federation",
        government = "Ward councils manage local services; a shared compact coordinates regional routes and common standards.",
        economy = "Commerce is spread across neighborhood markets, repair workshops, and small logistics cooperatives instead of one terminal district.",
        sectors = listOf("neighborhood commerce", "repair workshops", "regional logistics"),
        culture = "Districts keep their own gathering places while shared festivals connect the wider city.",
        traits = listOf("district festivals", "public market halls", "walking-street evenings"),
        techLevel = "Distributed urban infrastructure",
        technology = "Compact trams, timed bus corridors, and open civic maps connect the city without a megahub.",
        eventTitles = listOf("The Junction Plan Is Rejected", "The Local Routes Accord", "The District Market Charter"),
        middle = "Neighborhoods agree on a shared timetable and feeder routes between the separate centers of {place}.",
        late = "A district-market charter ties local producers to regional trade while keeping growth distributed.",
        locations = listOf(
            "The Civic Route Hall" to "Residents coordinate bus, tram, and walking routes across {place}.",
            "Old Terminal Commons" to "A proposed station site becomes a market square and community garden.",
            "District Workshop Row" to "Small repair shops cluster beside a local tram stop."
        ),
        anomalies = listOf(
            "The Unlisted Stop" to "A stop appears on hand-drawn maps, though no route passes it. People there describe a version of {place} that made a different choice.",
            "The Returning Timetable" to "A public timetable resets to a departure time removed from every city schedule decades ago."
        )
    ) else pattern(
        names = listOf("The Grand Concourse", "The Meridian Rail Compact", "The Continental Timetable"),
        summary = "A deliberate rail-first choice makes {place}, {country}, the region's principal interchange. Its {population} residents live around a dense network of lines, terminals, and station markets.",
        divergence = "A regional decision makes {place} the principal rail interchange for the surrounding territory.",
        system = "Regional transit authority with elected city wards",
        government = "A regional authority coordinates the rail spine; local wards govern neighborhoods and station areas.",
        economy = "Passenger services, freight, station retail, and engineering firms form the core of the urban economy.",
        sectors = listOf("rail engineering", "freight and passenger services", "station logistics"),
        culture = "Daily life follows the timetable: station plazas host civic gatherings, and neighborhoods welcome travelers.",
        traits = listOf("station-square festivals", "late-night food arcades", "regional commuter identity"),
        techLevel = "High-capacity electrified rail",
        technology = "Integrated signaling and scheduling move regional passengers and freight through the central interchange.",
        eventTitles = listOf("The Interchange Charter", "The First Through-Line", "The Regional Timetable"),
        middle = "A through-line links outlying towns directly to the main terminal, accelerating the growth of {place}.",
        late = "A unified timetable makes the central stations the region's main gateway for commerce.",
        locations = listOf(
            "The Meridian Terminal" to "The principal interchange where regional lines meet beneath a covered civic plaza.",
            "Signal House Nine" to "A preserved control room holding the first hand-marked regional schedule.",
            "The Through-Line Arcade" to "A station market connecting {place} to neighboring cities."
        ),
        anomalies = listOf(
            "The Platform Without a Line" to "An unlisted train stops at the terminal. Its board shows a district absent from every map.",
            "The Thirteenth Departure" to "Station clocks record a departure no crew remembers operating, yet a matching ticket appears in the archive."
        )
    )

    private fun governancePattern(denied: Boolean) = if (denied) pattern(
        names = listOf("The Ward Assembly", "The Neighborhood Charter", "The Civic Commons"),
        summary = "A rejected concentration of power leaves {place} governed through federated local assemblies. Its {population} residents share city services while wards retain strong control over schools, land, and public space.",
        divergence = "A proposed concentration of authority fails, and local assemblies become the basis of civic life.",
        system = "Federated ward assemblies",
        government = "Ward councils set local priorities; a rotating city forum coordinates shared budgets and resolves disputes.",
        economy = "Cooperatives, municipal services, and neighborhood businesses hold more influence than outside monopolies.",
        sectors = listOf("cooperatives", "municipal services", "neighborhood businesses"),
        culture = "Public debate is part of daily life. Wards keep local customs while making citywide decisions in open assemblies.",
        traits = listOf("open council days", "shared civic kitchens", "ward festivals"),
        techLevel = "Public-service technology",
        technology = "Shared registries and open civic networks make budgets and public records accessible.",
        eventTitles = listOf("The Ward Convention", "The Shared Services Pact", "The Open Budget Charter"),
        middle = "The wards of {place} sign a pact for shared water, schooling, and public records without giving up local authority.",
        late = "An open-budget charter lets residents trace how funds move between districts."
    ) else pattern(
        names = listOf("The Constitutional City", "The Civic Balance", "The Charter Government"),
        summary = "A durable constitutional settlement gives {place} stable administration with strong public oversight. Its {population} residents balance citywide planning with elected ward representation.",
        divergence = "A constitutional charter creates a new balance between citywide authority and local representation.",
        system = "Charter-based representative government",
        government = "An elected city cabinet handles shared services under a public charter; ward representatives review major plans.",
        economy = "Long-term public contracts support civic infrastructure, independent firms, and a broad service economy.",
        sectors = listOf("civic engineering", "independent services", "public infrastructure"),
        culture = "Civic ceremonies mark public works, while neighborhood associations preserve local traditions.",
        traits = listOf("charter anniversaries", "public works openings", "neighborhood associations"),
        techLevel = "Accountable civic systems",
        technology = "Open planning tools help residents review the city's long-term projects.",
        eventTitles = listOf("The Charter Convention", "The Public Review Act", "The Ward Representation Reform"),
        middle = "The Public Review Act requires plans for new construction in {place} to be published before work begins.",
        late = "A representation reform gives outer wards a formal voice in the annual investment plan."
    )

    private fun climatePattern(denied: Boolean) = if (denied) pattern(
        names = listOf("The Dryline Compact", "The Protected Shore", "The Watershed City"),
        summary = "An early intervention keeps severe flooding from defining {place}. Its {population} residents rely on wetlands, maintained channels, and shared watershed planning to protect the riverward districts.",
        divergence = "A citywide prevention plan is adopted before repeated floods reshape {place}.",
        system = "Regional watershed council and elected wards",
        government = "A watershed council coordinates upstream planning; local wards oversee public protection and maintenance.",
        economy = "Water engineering, maintained waterways, and flood-safe agriculture support the local economy.",
        sectors = listOf("water engineering", "river commerce", "flood-safe agriculture"),
        culture = "Annual river ceremonies celebrate the work that keeps neighborhoods dry and the waterways open.",
        traits = listOf("river-season calendars", "waterway clean-up days", "shared storm kitchens"),
        techLevel = "Preventive water infrastructure",
        technology = "Maintained channels, sensor buoys, and distributed barriers reduce flood risk before high-water seasons.",
        eventTitles = listOf("The Watershed Accord", "The Channel Works", "The Protected Shore Act"),
        middle = "Upstream settlements join {place} in funding a shared system of channels and wetland buffers.",
        late = "The Protected Shore Act makes upkeep of flood barriers and waterways a standing public service."
    ) else pattern(
        names = listOf("The High-Water Compact", "The Riverward Cities", "The Living Shoreline"),
        summary = "Repeated water threats reshape {place} into a city of raised commons, wetlands, and shared flood planning. Its {population} residents live with seasonal change through planned adaptation.",
        divergence = "An early water emergency changes how the city protects its river or coast.",
        system = "Regional watershed council and elected wards",
        government = "A watershed council coordinates upstream planning; local wards decide how shelter and adaptation funds are used.",
        economy = "Water management, wetland restoration, and seasonal trade support local employment.",
        sectors = listOf("water engineering", "wetland agriculture", "seasonal trade"),
        culture = "Annual water-mark ceremonies pass survival knowledge between generations.",
        traits = listOf("high-water ceremonies", "shared storm kitchens", "river-season calendars"),
        techLevel = "Climate-adaptive infrastructure",
        technology = "Floodable parks, modular barriers, and neighborhood warnings respond to changing water levels.",
        eventTitles = listOf("The Watershed Accord", "The First Retreat Plan", "The Living Shoreline Act"),
        middle = "River wards approve a retreat plan, turning exposed blocks into linked wetlands.",
        late = "The Living Shoreline Act makes restored marshland and raised paths standard for new construction."
    )

    private fun computingPattern(denied: Boolean) = if (denied) pattern(
        names = listOf("The Human-Scale Network", "The Public Terminal Era", "The Paper-and-Wire Compact"),
        summary = "Digital systems never become the default in {place}; public terminals, printed records, and neighborhood relays preserve access without one always-on network. Its {population} residents share a resilient information commons.",
        divergence = "A decision to limit ubiquitous digital networks redirects how people share information.",
        system = "Civic network trust with public oversight",
        government = "A public trust maintains communications, and ward librarians audit shared information systems.",
        economy = "Repair trades, publishing, public terminals, and local data cooperatives grow in place of platform monopolies.",
        sectors = listOf("device repair", "public publishing", "local data cooperatives"),
        culture = "Printed notices and neighborhood messengers remain trusted; important news is discussed face to face.",
        traits = listOf("bulletin exchanges", "public reading rooms", "community radio"),
        techLevel = "Selective, human-scale computing",
        technology = "Small local servers handle essential records while offline paper copies remain part of civic life.",
        eventTitles = listOf("The Public Network Charter", "The Offline Records Act", "The Neighborhood Relay Project"),
        middle = "The Offline Records Act keeps public records available on paper and at staffed terminals in {place}.",
        late = "Neighborhood relays link reading rooms without creating a central platform."
    ) else pattern(
        names = listOf("The Open Network City", "The Civic Mesh", "The Shared Signal"),
        summary = "A public-interest digital network becomes a core utility in {place}. Its {population} residents use a city-owned mesh for records, local trade, and communication, with neighborhood nodes keeping it accountable.",
        divergence = "A public network charter makes shared digital infrastructure a civic utility.",
        system = "Municipal network trust under elected oversight",
        government = "An elected board manages public infrastructure; neighborhood assemblies can challenge data policies.",
        economy = "Local software studios, data cooperatives, and network maintenance share the market with neighborhood businesses.",
        sectors = listOf("civic software", "data cooperatives", "network maintenance"),
        culture = "Digital commons are treated like public squares: communities set rules and keep archives open.",
        traits = listOf("open data days", "neighborhood mesh festivals", "community-run archives"),
        techLevel = "Mature civic network technology",
        technology = "A citywide mesh links open records, small-business exchanges, and community-built tools.",
        eventTitles = listOf("The Open Network Charter", "The First Civic Mesh", "The Shared Data Accord"),
        middle = "The first neighborhood mesh connects libraries, workshops, and public offices across {place}.",
        late = "The Shared Data Accord gives residents rights to inspect and correct city-held records."
    )

    private fun industryPattern(denied: Boolean) = if (denied) pattern(
        names = listOf("The Makers' Compact", "The Repair Economy", "The Open Workshop City"),
        summary = "Large centralized industry never dominates {place}. Small workshops, repair trades, and producer cooperatives support its {population} residents through a locally rooted economy.",
        divergence = "Large-scale industry fails to take hold, leaving room for small producers and repair trades.",
        system = "Municipal cooperative charter",
        government = "The city charter protects shared workshops, cooperative land trusts, and neighborhood business councils.",
        economy = "Repair, fabrication, local food production, and cooperative trade create a diverse economic base.",
        sectors = listOf("repair and reuse", "small-batch fabrication", "cooperative food production"),
        culture = "Residents take pride in making and mending; fairs exchange tools and practical skills.",
        traits = listOf("repair fairs", "shared tool libraries", "maker apprenticeships"),
        techLevel = "Modular and repairable technology",
        technology = "Open designs and standardized parts let local workshops maintain machinery without distant suppliers.",
        eventTitles = listOf("The Workshop Land Trust", "The Repair Rights Charter", "The Cooperative Exchange"),
        middle = "A workshop land trust keeps production spaces affordable for independent makers in {place}.",
        late = "The Cooperative Exchange links repair crews with food and fabrication cooperatives."
    ) else pattern(
        names = listOf("The Industrial Compact", "The Foundry Metropolis", "The New Works City"),
        summary = "A lasting industrial compact makes {place} a manufacturing center. Its {population} residents live among production districts, engineering schools, and worker institutions.",
        divergence = "Investment decisions establish {place} as a major manufacturing center.",
        system = "Industrial city government with worker councils",
        government = "The municipal council shares planning authority with elected worker boards in production districts.",
        economy = "Manufacturing, machine engineering, freight, and technical education drive growth and civic services.",
        sectors = listOf("manufacturing", "machine engineering", "technical education"),
        culture = "Shift changes shape the daily rhythm; worker halls and technical institutes anchor public life.",
        traits = listOf("foundry-day parades", "worker halls", "technical apprenticeships"),
        techLevel = "Advanced industrial engineering",
        technology = "Automated machine works, public power stations, and freight systems support large-scale production.",
        eventTitles = listOf("The Foundry Charter", "The Workers' Council Act", "The Regional Works Plan"),
        middle = "The Workers' Council Act gives production districts a role in safety and investment decisions.",
        late = "A regional works plan connects factories with technical schools and public power."
    )

    private fun culturePattern(denied: Boolean) = if (denied) pattern(
        names = listOf("The Many Voices Charter", "The Open Tongues City", "The Shared Language Compact"),
        summary = "A single official culture never becomes the only route into public life in {place}. Its {population} residents share a multilingual civic sphere shaped by several traditions.",
        divergence = "A single official cultural standard is rejected, and public institutions make room for several traditions.",
        system = "Plural civic charter",
        government = "Public institutions serve several language and cultural communities through locally elected advisory boards.",
        economy = "Translation, cultural production, neighborhood services, and trade across communities form a broad economy.",
        sectors = listOf("cultural production", "translation and education", "community services"),
        culture = "Several languages and traditions share civic spaces; festivals rotate across neighborhoods.",
        traits = listOf("rotating neighborhood festivals", "multilingual signs", "shared music halls"),
        techLevel = "Accessible multilingual public systems",
        technology = "Notices, school materials, and archives are maintained in the city's major community languages.",
        eventTitles = listOf("The Many Voices Charter", "The Shared Schools Agreement", "The Public Language Act"),
        middle = "The Shared Schools Agreement funds local-language instruction alongside a common civic curriculum.",
        late = "A public language act makes city services available through community translators."
    ) else pattern(
        names = listOf("The Common Festival City", "The Civic Arts Compact", "The Shared Heritage Quarter"),
        summary = "An arts and education movement reshapes public life in {place}. Its {population} residents share festivals and archives while neighborhood groups keep distinctive traditions alive.",
        divergence = "A citywide arts and education movement changes how public culture is supported.",
        system = "Elected city government with neighborhood cultural councils",
        government = "Neighborhood cultural councils advise the elected government on education, public spaces, and heritage funding.",
        economy = "Public arts, education, independent publishing, and local tourism complement trade and services.",
        sectors = listOf("public arts", "education", "independent publishing"),
        culture = "Public art and shared festivals create a civic calendar without erasing neighborhood traditions.",
        traits = listOf("open-air performances", "shared heritage days", "neighborhood art walks"),
        techLevel = "Open civic archives",
        technology = "Libraries and cultural halls maintain accessible performance records and oral histories.",
        eventTitles = listOf("The Civic Arts Fund", "The Open Stage Movement", "The Shared Archive Project"),
        middle = "A public arts fund gives every district a venue for performance and local history.",
        late = "A shared archive opens neighborhood photographs, histories, and newspapers to the public."
    )

    private val civicPattern = pattern(
        names = listOf("The Civic Commons", "The Open City Compact", "The Lantern Accord"),
        summary = "A different civic compact changes how {place} grows. Its {population} residents balance neighborhood stewardship with shared city services and practical cooperation.",
        divergence = "A different civic decision gives neighborhood stewards a lasting voice in how {place} grows.",
        system = "Civic compact with elected ward councils",
        government = "Ward councils coordinate public services through a compact that keeps major decisions open to residents.",
        economy = "Local commerce, public infrastructure, and independent workshops support a mixed urban economy.",
        sectors = listOf("local commerce", "public infrastructure", "independent workshops"),
        culture = "Public squares connect neighborhoods while preserving local traditions.",
        traits = listOf("open-square evenings", "neighborhood archives", "seasonal city festivals"),
        techLevel = "Practical civic technology",
        technology = "Open maps, public records, and maintained local infrastructure keep services accessible.",
        eventTitles = listOf("The Civic Compact", "The Shared Square Plan", "The Neighborhood Archive Act"),
        middle = "The Shared Square Plan creates public gathering spaces across {place}.",
        late = "The Neighborhood Archive Act funds local collections and shares their records across the city."
    )

    private fun pattern(
        names: List<String>, summary: String, divergence: String, system: String, government: String,
        economy: String, sectors: List<String>, culture: String, traits: List<String>, techLevel: String,
        technology: String, eventTitles: List<String>, middle: String, late: String,
        locations: List<Pair<String, String>>? = null,
        anomalies: List<Pair<String, String>>? = null
    ): Pattern {
        val worldClues = "$summary $techLevel $economy".lowercase()
        val mobility = worldClues.contains("rail")
        val climate = worldClues.contains("water") || worldClues.contains("flood") || worldClues.contains("river")
        val computing = worldClues.contains("digital") || worldClues.contains("network") || worldClues.contains("terminal")
        val industry = worldClues.contains("industry") || worldClues.contains("manufacturing") || worldClues.contains("workshop")
        val hasCultureClues = worldClues.contains("culture") || worldClues.contains("language") || worldClues.contains("arts")
        val generatedLocations = when {
            mobility -> listOf(
                "The Civic Route Hall" to "Residents coordinate local connections and regional routes across {place}.",
                "The Old Terminal Commons" to "A former transport site becomes a market square and public garden.",
                "The Signal Archive" to "A public collection preserves the maps and schedules that shaped the city."
            )
            climate -> listOf(
                "The Floodline Gardens" to "Wetlands mark the old waterline and buffer the riverward neighborhoods.",
                "High-Water Hall" to "A raised public shelter where seasonal forecasts are shared.",
                "The River Steps" to "A landing that rises with the water while keeping a route open through {place}."
            )
            computing -> listOf(
                "The Common Reading Room" to "A public terminal and printed archive serve residents across {place}.",
                "Relay House" to "A neighborhood communications room for public notices and community records.",
                "The Open Data Hall" to "Residents inspect the public systems that support everyday city services."
            )
            industry -> listOf(
                "The Tool Library" to "A lending hall where residents borrow equipment and learn maintenance skills.",
                "Foundry Lane" to "Small production studios and repair shops share a reclaimed industrial block.",
                "The Cooperative Exchange" to "A market where independent makers trade tools, materials, and designs."
            )
            hasCultureClues -> listOf(
                "The Voices Hall" to "A performance and meeting space shared by the communities of {place}.",
                "The Language Library" to "A public archive of newspapers and recordings in the city's languages.",
                "Common Table Market" to "A covered market where food and craft traditions meet."
            )
            else -> listOf(
                "The Civic Exchange" to "Residents and ward representatives resolve shared city questions here.",
                "Lantern Quarter" to "A close-knit district known for evening workshops and community tables.",
                "The Open Archive" to "A city record room for neighborhood maps, letters, and oral histories."
            )
        }
        val generatedAnomalies = when {
            mobility -> listOf(
                "The Unlisted Stop" to "A stop appears on hand-drawn maps, though no route passes it. People there describe a version of {place} that made a different choice.",
                "The Returning Timetable" to "A public timetable resets to a departure time removed from every city schedule decades ago."
            )
            climate -> listOf(
                "The Second Tide Mark" to "A high-water line appears above the measured flood level, dated several years ahead.",
                "The Dry Bell" to "A warning bell rings on still nights before instruments detect a change in the water."
            )
            computing -> listOf(
                "The Future Bulletin" to "A public notice describes an event in {place} several days before it happens.",
                "The Unsent Message" to "An archived message appears in every copy of the public record, though no one remembers sending it."
            )
            industry -> listOf(
                "The Returning Tool" to "A tool lost in one workshop keeps turning up on another maker's bench in {place}.",
                "The Self-Repairing Plan" to "A workshop blueprint gains a new part each time it is copied."
            )
            hasCultureClues -> listOf(
                "The Untranslated Sign" to "A sign appears in a language no living resident recognizes, yet many can read it.",
                "The Echo Chorus" to "A public square repeats a song in a different language each evening, though no speaker is found."
            )
            else -> listOf(
                "The Reappearing Lane" to "A narrow lane disappears from every map at noon, then returns after the evening bell.",
                "The Second Record" to "A public archive contains a signed record of an event that official history says never happened in {place}."
            )
        }
        return Pattern(names, summary, divergence, system, government, economy, sectors, culture, traits,
            techLevel, technology, eventTitles, middle, late,
            locations ?: generatedLocations, anomalies ?: generatedAnomalies)
    }

    private fun eventYears(divergenceYear: Int, currentYear: Int): List<Int> {
        val span = (currentYear - divergenceYear).coerceAtLeast(0)
        return listOf(divergenceYear, divergenceYear + span / 2, currentYear).distinct()
    }

    private fun historicalSignificance(theme: Theme, denied: Boolean, eventIndex: Int, place: String): String {
        val consequence = when (theme) {
            Theme.MOBILITY -> if (denied) {
                "It keeps transport investment distributed among neighborhoods, shaping where work, trade, and public life take root."
            } else {
                "It concentrates regional movement around the interchange, drawing jobs, commerce, and civic influence toward the stations."
            }
            Theme.GOVERNANCE -> if (denied) {
                "It makes local representation a durable source of authority and changes how shared services are negotiated."
            } else {
                "It establishes a lasting balance between citywide planning and representative oversight."
            }
            Theme.CLIMATE -> if (denied) {
                "It makes prevention and shared watershed maintenance central to the city's safety and long-term growth."
            } else {
                "It turns adaptation to high water into a permanent part of public planning and neighborhood life."
            }
            Theme.COMPUTING -> if (denied) {
                "It preserves public access through human-scale systems and keeps information infrastructure accountable."
            } else {
                "It makes shared digital infrastructure a civic institution with lasting effects on access and public records."
            }
            Theme.INDUSTRY -> if (denied) {
                "It protects a locally rooted economy and distributes productive power among workshops and cooperatives."
            } else {
                "It anchors livelihoods and political influence in large-scale production and its worker institutions."
            }
            Theme.CULTURE -> if (denied) {
                "It gives several communities a durable place in public institutions and the city's shared identity."
            } else {
                "It makes shared cultural spaces a lasting part of civic life while reshaping how local traditions are preserved."
            }
            Theme.CIVIC -> if (denied) {
                "It gives residents a continuing role in local decisions and in the stewardship of shared services."
            } else {
                "It creates a durable civic framework for balancing neighborhood priorities with shared city needs."
            }
        }

        return when (eventIndex) {
            0 -> "This turning point sets the direction for $place: $consequence"
            1 -> "As the new arrangement spreads, it changes who benefits from growth and how the city organizes essential services. $consequence"
            else -> "Its long-term legacy is visible in today's institutions, economy, and everyday life. $consequence"
        }
    }

    private fun locationImportance(theme: Theme, denied: Boolean, locationIndex: Int, place: String): String {
        val legacy = when (theme) {
            Theme.MOBILITY -> if (denied) "distributed routes keep neighboring districts connected without a single dominant terminal" else "the rail interchange concentrates movement, commerce, and civic influence"
            Theme.GOVERNANCE -> if (denied) "ward assemblies keep local authority visible in the city's shared decisions" else "the charter makes this place part of the balance between citywide planning and public oversight"
            Theme.CLIMATE -> if (denied) "preventive watershed planning helps protect residents before dangerous high water" else "the site shows how adapting to seasonal water reshaped public space and city planning"
            Theme.COMPUTING -> if (denied) "public access to information remains available beyond a centralized digital network" else "shared civic technology makes city services and public records more accessible"
            Theme.INDUSTRY -> if (denied) "local workshops and cooperatives preserve a broad base of neighborhood production" else "industrial investment links local livelihoods to the city's worker institutions"
            Theme.CULTURE -> if (denied) "several communities can take part in public life without giving up their traditions" else "shared cultural spaces help the city preserve and exchange local histories"
            Theme.CIVIC -> if (denied) "residents retain a practical role in shaping common services and neighborhood life" else "the civic compact gives the city a durable way to balance local and shared needs"
        }

        return when (locationIndex) {
            0 -> "This is one of the places where $place's turning point became part of everyday life: $legacy."
            1 -> "Its changed purpose makes the alternate timeline tangible: $legacy."
            else -> "This place preserves the systems and shared memory that let the new order endure: $legacy."
        }
    }

    private fun populationFor(theme: Theme, denied: Boolean, seed: Long): Long {
        val (base, spread) = when {
            theme == Theme.MOBILITY && denied -> 3_200_000L to 5_000_000L
            theme == Theme.MOBILITY -> 11_000_000L to 9_000_000L
            else -> 2_000_000L to 10_000_000L
        }
        return base + seed % spread
    }

    private fun formatPopulation(population: Long) =
        "%.1f million".format(java.util.Locale.US, population / 1_000_000.0)

    private data class CharacterProfile(
        val age: Int,
        val role: String,
        val background: String,
        val personality: String,
        val timelineContribution: String,
        val majorActions: List<String>
    )

    private fun charactersFor(
        theme: Theme,
        denied: Boolean,
        variant: Int,
        place: String,
        country: String
    ): List<WorldCharacter> {
        val names = characterNames(country)
        val profiles = characterProfiles(theme)
        val timelineChange = timelineChange(theme, denied, place)

        return profiles.mapIndexed { index, profile ->
            val name = names[(index + variant) % names.size]
            WorldCharacter(
                name = name,
                role = profile.role,
                age = profile.age,
                background = profile.background.replace("{place}", place),
                personality = profile.personality,
                timelineRelationship = "$name's work became significant when $timelineChange; ${profile.timelineContribution}.",
                majorActions = profile.majorActions.map { it.replace("{place}", place) }
            )
        }
    }

    private fun characterNames(country: String): List<String> = when {
        country.contains("japan", ignoreCase = true) -> listOf("Aiko Tanaka", "Kenji Sato", "Yumi Nakamura")
        country.contains("india", ignoreCase = true) -> listOf("Meera Rao", "Arjun Nair", "Farah Khan")
        country.contains("iceland", ignoreCase = true) -> listOf("Sigríður Jónsdóttir", "Einar Guðmundsson", "Katrín Björnsdóttir")
        country.contains("portugal", ignoreCase = true) -> listOf("Inês Silva", "João Costa", "Marta Ferreira")
        else -> listOf("Amara Okafor", "Daniel Reyes", "Lina Petrov")
    }

    private fun characterProfiles(theme: Theme): List<CharacterProfile> = when (theme) {
        Theme.MOBILITY -> listOf(
            CharacterProfile(46, "Transit planner", "A civil engineer who moved from road maintenance into route planning after repeated complaints from outer districts of {place}.", "Patient with field data, but skeptical of plans that ignore the last stretch of a journey.", "used passenger counts to argue for reliable transfers", listOf("Mapped weekday transfer gaps between outer districts of {place}.", "Ran public route trials before the new timetable was adopted.", "Kept a plain-language record of service changes for neighborhood councils.")),
            CharacterProfile(39, "Transit dispatcher", "Learned operations on evening shifts, coordinating drivers and responding to missed connections across {place}.", "Quick under pressure and generous with practical advice; reluctant to take credit.", "adjusted shift handoffs so the changing routes stayed usable for workers", listOf("Coordinated late-shift connections between employment districts.", "Created a paper fallback schedule for service interruptions.", "Trained new dispatchers to report delays consistently.")),
            CharacterProfile(58, "Market cooperative coordinator", "Spent decades organizing deliveries for small vendors whose livelihoods depend on predictable journeys through {place}.", "Direct, observant, and protective of small traders' time.", "represented local vendors when transport decisions affected market access", listOf("Arranged shared delivery windows for neighborhood markets.", "Negotiated loading access without displacing nearby stalls.", "Recorded which route changes helped independent shops."))
        )
        Theme.GOVERNANCE -> listOf(
            CharacterProfile(52, "Municipal records clerk", "Started as a filing assistant and became responsible for keeping public decisions searchable and complete in {place}.", "Careful, quietly persistent, and uncomfortable with vague minutes.", "made meeting records available to residents outside the central offices", listOf("Rebuilt a searchable index of ward decisions.", "Flagged budget entries that lacked a public explanation.", "Helped residents request records without hiring a representative.")),
            CharacterProfile(43, "Ward meeting facilitator", "Returned to public work after years coordinating a neighborhood tenants' association in {place}.", "A patient listener who will interrupt when quieter people are being ignored.", "helped neighboring wards turn disagreements into a shared services agreement", listOf("Scheduled open meetings in districts rarely reached by city staff.", "Set up a rotating speaking order for contested hearings.", "Recorded follow-up tasks so agreements did not vanish after the meeting.")),
            CharacterProfile(31, "Public school administrator", "Managed a small neighborhood school and learned how decisions about land and services affect families in {place}.", "Practical and empathetic, with little interest in ceremony.", "translated citywide policy changes into routines families could understand", listOf("Kept school schedules aligned with new public-service arrangements.", "Organized evening information sessions for caregivers.", "Connected school staff with ward services during budget changes."))
        )
        Theme.CLIMATE -> listOf(
            CharacterProfile(45, "Waterworks inspector", "Joined the maintenance crews that check channels and pumping equipment before each high-water season in {place}.", "Methodical, calm in a crisis, and willing to report a small fault early.", "turned local water observations into practical maintenance priorities", listOf("Logged channel blockages before the seasonal rains.", "Trained local crews to use a shared inspection checklist.", "Marked safe foot routes when water levels changed.")),
            CharacterProfile(37, "Community clinic nurse", "Works at a riverward clinic and has spent several emergency seasons helping residents plan for disrupted access in {place}.", "Steady and reassuring, but insists that warnings include people with limited mobility.", "made public safety plans account for the needs of ordinary households", listOf("Prepared medication and transport plans for flood-prone blocks.", "Set up a check-in rota with older residents.", "Reported which shelters could be reached during heavy rain.")),
            CharacterProfile(61, "Waterside grower", "Inherited a small family plot and adapted its planting calendar as water levels and city rules changed around {place}.", "Independent, wry, and generous with hard-won seasonal knowledge.", "shared on-the-ground observations with planners before changes were finalized", listOf("Tested crops suited to the changing water table.", "Shared seasonal notes with the watershed council.", "Kept a seed store for neighboring growers after difficult years."))
        )
        Theme.COMPUTING -> listOf(
            CharacterProfile(50, "Public librarian", "Has spent most of a career helping residents find records, notices, and practical information in {place}.", "Curious and even-tempered; believes information should be easy to verify.", "kept essential records accessible as the city's communication systems changed", listOf("Maintained a printed backup of high-use civic records.", "Taught residents how to check the source of public notices.", "Helped set opening hours around shift workers' schedules.")),
            CharacterProfile(35, "Device repair technician", "Learned electronics in a family repair shop and now services equipment used by public offices and neighborhood groups in {place}.", "Inventive, unpretentious, and wary of equipment no one can repair locally.", "kept everyday communication tools working through changes to the public network", listOf("Repaired terminals and radios using locally available parts.", "Published a simple maintenance guide for community groups.", "Recovered public files from devices headed for disposal.")),
            CharacterProfile(29, "Civic records assistant", "Joined the municipal archive on a temporary contract and stayed to improve how residents request city records in {place}.", "Organized and tactful, with a strong sense of what should remain public.", "helped apply the timeline's new rules for access to information", listOf("Catalogued duplicate public records before digitization.", "Tested request forms with residents unfamiliar with city offices.", "Documented which records needed an offline copy."))
        )
        Theme.INDUSTRY -> listOf(
            CharacterProfile(49, "Machine fitter", "Trained in a small workshop and later maintained production equipment used across {place}.", "Practical, patient with apprentices, and quick to notice unsafe shortcuts.", "kept production changes grounded in the needs of the people maintaining equipment", listOf("Standardized common replacement parts across local workshops.", "Reported recurring safety issues to the worker committee.", "Trained two apprentices in repair before replacement.")),
            CharacterProfile(40, "Cooperative bookkeeper", "Started keeping accounts for a food co-op and now helps independent producers understand shared costs in {place}.", "Exact about numbers but approachable when explaining them.", "made it easier for smaller producers to participate in the changing economy", listOf("Introduced a shared ledger for cooperative purchases.", "Helped small producers plan for seasonal cash-flow gaps.", "Published plain-language summaries of cooperative dues.")),
            CharacterProfile(24, "Technical college instructor", "Returned to the city after an apprenticeship elsewhere and teaches maintenance skills at a public technical college in {place}.", "Enthusiastic about teaching and honest about what a machine cannot do.", "connected local workers to the skills needed for the city's economic shift", listOf("Built a paid evening course for working adults.", "Arranged supervised placements with local employers.", "Updated lessons when equipment standards changed."))
        )
        Theme.CULTURE -> listOf(
            CharacterProfile(44, "Community language teacher", "Teaches evening classes and helps families navigate school and public notices in more than one language in {place}.", "Warm, precise, and attentive to meanings lost in a literal translation.", "helped residents take part in public institutions across language differences", listOf("Prepared translated guides for public meetings.", "Started a reading group for parents and teenagers.", "Reviewed school notices with families before distribution.")),
            CharacterProfile(36, "Community hall technician", "Maintains sound and lighting at a modest public hall where local groups rehearse, meet, and hold celebrations in {place}.", "Resourceful and sociable, though happiest behind the scenes.", "kept shared cultural spaces usable for groups with different needs", listOf("Set up a low-cost booking rota for neighborhood groups.", "Repaired donated sound equipment instead of replacing it.", "Recorded oral histories during annual community gatherings.")),
            CharacterProfile(63, "Neighborhood archive volunteer", "Retired from school administration and now labels photographs and oral histories donated by families in {place}.", "Patient, warm, and careful about asking permission before sharing a story.", "preserved local memories as the city's shared cultural institutions changed", listOf("Collected captions from residents for an old street-photo archive.", "Recorded interviews with shopkeepers facing redevelopment.", "Helped families decide which recordings could be made public."))
        )
        Theme.CIVIC -> listOf(
            CharacterProfile(48, "Public housing caretaker", "Has maintained the shared rooms and noticeboards of a housing block in {place} through several rounds of civic reform.", "Observant and dependable, with a dry sense of humor.", "made new civic rules understandable to residents who did not work in city offices", listOf("Kept a public noticeboard current and translated jargon into plain language.", "Organized a repair rota for shared spaces.", "Collected resident questions before district meetings.")),
            CharacterProfile(34, "Neighborhood association secretary", "Balances a day job with volunteer minutes and correspondence for a mixed-use district group in {place}.", "Conscientious, diplomatic, and willing to ask for a decision in writing.", "carried neighborhood concerns into the forums shaping the new civic compact", listOf("Set up a small-grants calendar residents could follow.", "Recorded unresolved issues between quarterly meetings.", "Brought shopkeepers and tenants into the same planning session.")),
            CharacterProfile(27, "City survey assistant", "Joined a municipal field team after studying land records and now checks street-level details before projects are approved in {place}.", "Curious, methodical, and open to correcting an early assumption.", "connected the city's plans to how public spaces were actually used", listOf("Checked public-space plans against on-site measurements.", "Logged access problems reported by wheelchair users.", "Published corrected neighborhood maps after resident review."))
        )
    }

    private fun timelineChange(theme: Theme, denied: Boolean, place: String): String = when (theme) {
        Theme.MOBILITY -> if (denied) "the region chose district routes instead of making $place its single rail hub" else "the region concentrated rail travel through $place"
        Theme.GOVERNANCE -> if (denied) "local wards in $place retained authority over shared services" else "a charter in $place formalized public oversight and citywide planning"
        Theme.CLIMATE -> if (denied) "officials in $place invested in preventing flood damage before it became a defining crisis" else "repeated high-water emergencies pushed $place toward adaptation"
        Theme.COMPUTING -> if (denied) "public leaders in $place chose limits on ubiquitous digital networks" else "a public digital network became part of $place's civic infrastructure"
        Theme.INDUSTRY -> if (denied) "large factories did not become $place's dominant source of work" else "regional manufacturing became central to $place's economy"
        Theme.CULTURE -> if (denied) "public institutions in $place kept several cultural traditions in civic life" else "a shared arts and education movement reshaped public culture in $place"
        Theme.CIVIC -> if (denied) "neighborhood stewards in $place kept a say in local decisions" else "a civic compact in $place gave ward councils a lasting role"
    }

    private fun anomalySeeds(theme: Theme, denied: Boolean): List<Pair<String, String>> = when (theme) {
        Theme.MOBILITY -> if (denied) listOf(
            "The Unlisted Stop" to "A stop appears on hand-drawn maps of {place}, though current routes and field surveys find no platform.",
            "The Returning Timetable" to "A public schedule in {place} occasionally displays a departure time removed from the archive years ago."
        ) else listOf(
            "The Platform Without a Line" to "An unlisted platform appears on terminal diagrams for {place}, but no track leads to it.",
            "The Thirteenth Departure" to "Station clocks in {place} record a departure absent from every operator log, though its ticket is valid stock."
        )
        Theme.GOVERNANCE -> if (denied) listOf(
            "The Uncounted Vote" to "Minutes from open ward meetings in {place} sometimes show a final tally one vote different from the signed attendance sheet.",
            "The Second Seal" to "A public copy of a ward agreement in {place} bears a second civic seal not present on the original."
        ) else listOf(
            "The Ward Without a Boundary" to "Two approved planning maps in {place} assign the same block to different wards, and both match current street signs.",
            "The Speaking Ledger" to "A digitized hearing register in {place} lists one speaker before their name was added to the sign-in book."
        )
        Theme.CLIMATE -> if (denied) listOf(
            "The Second Tide Mark" to "A high-water mark appears above the measured level along {place}'s river, dated several years ahead.",
            "The Dry Bell" to "A public water-level bell in {place} rings on still nights before any nearby gauge records a change."
        ) else listOf(
            "Tomorrow's Rain Ledger" to "A local gauge archive in {place} contains precise rainfall readings dated one day in the future.",
            "The High-Water Photograph" to "After each seasonal rise, the same empty riverside steps appear wet in a photograph taken during clear weather."
        )
        Theme.COMPUTING -> if (denied) listOf(
            "The Future Bulletin" to "A printed public notice in {place} describes a civic event several days before it happens.",
            "The Unsent Message" to "The same unsigned message appears in separate offline record copies in {place}, though none has a sender or delivery mark."
        ) else listOf(
            "The Early Acknowledgment" to "A public network node in {place} sometimes records a reply seconds before the original message arrives.",
            "The Vanishing Notice" to "A civic notice in {place} is acknowledged by dozens of residents, then disappears from every public archive copy."
        )
        Theme.INDUSTRY -> if (denied) listOf(
            "The Returning Tool" to "A missing workshop tool in {place} repeatedly turns up on a different cooperative's bench, still bearing its first maker's mark.",
            "The Self-Repairing Plan" to "A machine blueprint in {place} gains a small but workable repair step each time it is copied."
        ) else listOf(
            "The Idle Shift" to "Production logs in {place} show a complete shift's output on a night when power records and attendance both show the factory was closed.",
            "The Traveling Blueprint" to "Identical workshop plans in {place} acquire handwritten revisions in different archives, in the same unfamiliar hand."
        )
        Theme.CULTURE -> if (denied) listOf(
            "The Untranslated Sign" to "A sign appears in a language no catalogued community in {place} recognizes, yet several residents understand it differently.",
            "The Echo Chorus" to "A public square in {place} carries a familiar melody in a different language each evening, with no nearby performance."
        ) else listOf(
            "The Final Verse" to "Recordings of the same public concert in {place} preserve different final verses, each confirmed by its audience.",
            "The Absent Pavilion" to "A donated festival photograph shows a pavilion in {place} that appears on no construction plan or neighborhood memory."
        )
        Theme.CIVIC -> if (denied) listOf(
            "The Reappearing Lane" to "A narrow lane in {place} is missing from midday maps, then returns on the same route after the evening bell.",
            "The Second Record" to "A city archive in {place} holds a signed account of a public decision absent from the official meeting minutes."
        ) else listOf(
            "The Neighboring Address" to "Two separate homes in {place} receive mail bearing the same official address, and each delivery route is correct.",
            "The Unscheduled Inspection" to "A maintenance log in {place} records a routine street inspection completed before it was assigned."
        )
    }

    private fun anomaliesFor(
        theme: Theme,
        denied: Boolean,
        place: String,
        seeds: List<Pair<String, String>>
    ): List<WorldAnomaly> = seeds.mapIndexed { index, (name, description) ->
        WorldAnomaly(
            name = name,
            classification = anomalyClassification(theme, index),
            description = description.replace("{place}", place),
            discovery = anomalyDiscovery(theme, denied, index, place),
            knownEffects = anomalyEffects(theme, index),
            currentStatus = if (index == 0) "Documented · under observation" else "Intermittent · comparing records"
        )
    }

    private fun anomalyClassification(theme: Theme, index: Int): String = when (theme) {
        Theme.MOBILITY -> if (index == 0) "Map / route variance" else "Schedule discontinuity"
        Theme.GOVERNANCE -> if (index == 0) "Record discrepancy" else "Document duplication"
        Theme.CLIMATE -> if (index == 0) "Hydrological irregularity" else "Forecast divergence"
        Theme.COMPUTING -> if (index == 0) "Time-stamped broadcast" else "Network record anomaly"
        Theme.INDUSTRY -> if (index == 0) "Material recurrence" else "Plan alteration"
        Theme.CULTURE -> if (index == 0) "Linguistic artifact" else "Cultural record variance"
        Theme.CIVIC -> if (index == 0) "Spatial inconsistency" else "Archival contradiction"
    }

    private fun anomalyDiscovery(theme: Theme, denied: Boolean, index: Int, place: String): String = when (theme) {
        Theme.MOBILITY -> if (index == 0) "A district route survey compared paper maps with walking inspections in $place and found the same marked stop on several unrelated copies." else "A timetable archivist noticed the entry during a routine comparison of current and retired schedules."
        Theme.GOVERNANCE -> if (index == 0) "A records clerk noticed the mismatch while reconciling signed attendance sheets from $place's public meetings." else "The anomaly surfaced during a ward clerk's annual review of sealed agreements and scanned copies."
        Theme.CLIMATE -> if (index == 0) "Watershed staff noticed it while checking seasonal gauge readings against older marks along the $place waterline." else "A local station technician flagged the entry during routine forecast and instrument maintenance."
        Theme.COMPUTING -> if (denied) "A librarian found it while maintaining the paper-and-terminal public record in $place." else "Network maintainers noticed the discrepancy while reviewing ordinary service logs from public nodes in $place."
        Theme.INDUSTRY -> if (index == 0) "Workshop staff first reported it while reconciling inventory and maintenance notes from $place." else "A technical college instructor noticed the revision while comparing donated copies of a standard shop plan."
        Theme.CULTURE -> if (index == 0) "A language teacher brought it to the neighborhood archive after residents gave conflicting but confident readings." else "The record surfaced while a community hall technician catalogued material from a public arts event in $place."
        Theme.CIVIC -> if (index == 0) "City survey assistants flagged the route mismatch while checking neighborhood maps against current street access in $place." else "An archive volunteer found the discrepancy while indexing routine public works records."
    }

    private fun anomalyEffects(theme: Theme, index: Int): List<String> = when (theme) {
        Theme.MOBILITY -> if (index == 0) listOf("Route planners cannot locate a physical stop at the marked position.", "Residents use it as a reference point when comparing old neighborhood maps.") else listOf("The listed departure has no matching train or crew record.", "The timetable returns to normal after the next scheduled update.")
        Theme.GOVERNANCE -> if (index == 0) listOf("The extra vote changes no adopted decision so far.", "The signed attendance and individual ballots remain consistent with one another.") else listOf("Both copies pass ordinary document checks.", "No ward has changed its boundary or agreement based on the second seal.")
        Theme.CLIMATE -> if (index == 0) listOf("Nearby gauges do not confirm the higher water mark.", "The date does not align with any recorded storm or flood.") else listOf("The entry has not helped forecasters predict measurable rain.", "Nearby instruments continue to report current local conditions normally.")
        Theme.COMPUTING -> if (index == 0) listOf("The time difference appears only in archived notice copies.", "No public service has acted on the early information.") else listOf("Residents can confirm seeing the notice, but no copy can be recovered.", "Routine network and record services continue without interruption.")
        Theme.INDUSTRY -> if (index == 0) listOf("No finished goods matching the log entry have been found.", "The event has not affected current stock or production schedules.") else listOf("The revisions are mechanically sound but have no known author.", "No workshop has received a physical plan with those changes.")
        Theme.CULTURE -> if (index == 0) listOf("Each audience remembers its own version clearly.", "No recording equipment fault has explained the differences.") else listOf("The pavilion has not been found in the event grounds or city archive.", "The photograph shows no sign of editing in its original negative.")
        Theme.CIVIC -> if (index == 0) listOf("Both addresses work for mail delivery and municipal service requests.", "The records system accepts each home without a duplicate warning.") else listOf("The inspection notes describe ordinary street maintenance.", "No inspector recalls being assigned or visiting at the recorded time.")
    }
}
