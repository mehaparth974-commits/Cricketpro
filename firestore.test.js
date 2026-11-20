const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "anu61284@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("players").where("privateStats", "==", false).get());
  await assertFails(unauthDb.collection("matches").where("publicVisible", "==", true).get());
  await assertFails(
    unauthDb.collection("teams").doc("team_1").set({
      name: "Unauth XI",
      shortName: "UXI",
      ownerUid: ALICE_UID,
      captainName: "Alice",
      playerNames: ["Alice"],
      publicVisible: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Player profile: owner can create/update, role escalation fails, PII is isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate(5000);
  // Role escalation must fail
  await assertFails(
    aliceDb.collection("players").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice_bat",
      battingStyle: "Right-Hand Bat",
      bowlingStyle: "Right-Arm Medium",
      playingRole: "All-Rounder",
      privateStats: false,
      role: "admin",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid create succeeds (no @DocumentId `id` field)
  await assertSucceeds(
    aliceDb.collection("players").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice_bat",
      battingStyle: "Right-Hand Bat",
      bowlingStyle: "Right-Arm Medium",
      playingRole: "All-Rounder",
      privateStats: false,
      role: "user",
      followersCount: 0,
      matchesPlayed: 0,
      runsScored: 0,
      ballsFaced: 0,
      highestScore: 0,
      fifties: 0,
      hundreds: 0,
      fours: 0,
      sixes: 0,
      wicketsTaken: 0,
      runsConceded: 0,
      ballsBowled: 0,
      bestBowlingWickets: 0,
      bestBowlingRuns: 0,
      catches: 0,
      stumpings: 0,
      runOuts: 0,
      mvpPoints: 0,
      badges: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Private PII doc only accessible by owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      phone: "+15550199",
      notificationsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );

  // Filtered discovery succeeds; unfiltered list fails
  await assertSucceeds(
    bobDb.collection("players").where("privateStats", "==", false).get()
  );
  await assertFails(bobDb.collection("players").get());
});

test("Email spoofing with email_verified=false cannot access admin privileges", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofAdminDb.collection("app_config").doc("global").set({
      maintenanceMode: true,
      announcementBanner: "Hacked",
      defaultOvers: 20,
      allowCommunityPosts: true,
      updatedBy: "spoof_1",
      updatedAt: pastDate(),
    })
  );

  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    realAdminDb.collection("app_config").doc("global").set({
      maintenanceMode: false,
      announcementBanner: "Season 2026 Live",
      defaultOvers: 20,
      allowCommunityPosts: true,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(),
    })
  );
});

test("Match & ball scoring: scorer writes balls, terminal lock protects completed scores", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("matches").doc("match_1").set({
      teamAId: "team_a",
      teamBId: "team_b",
      teamAName: "Emerald Strikers",
      teamBName: "Obsidian Royals",
      teamAShort: "EMS",
      teamBShort: "OBR",
      teamAPlayers: ["Alice", "A2"],
      teamBPlayers: ["Bob", "B2"],
      oversLimit: 20,
      powerplayOvers: 6,
      groundName: "Central Turf",
      tossWinner: "Emerald Strikers",
      tossDecision: "BAT",
      battingFirstTeam: "A",
      status: "LIVE",
      currentInnings: 1,
      teamARuns: 0,
      teamAWickets: 0,
      teamABalls: 0,
      teamBRuns: 0,
      teamBWickets: 0,
      teamBBalls: 0,
      targetRuns: 0,
      resultText: "Match in progress",
      scorerUid: ALICE_UID,
      publicVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Scorer adds ball event
  await assertSucceeds(
    aliceDb.collection("matches").doc("match_1").collection("balls").doc("ball_0").set({
      matchId: "match_1",
      scorerUid: ALICE_UID,
      publicVisible: true,
      sequenceIndex: 0,
      innings: 1,
      overNumber: 0,
      ballInOver: 1,
      strikerName: "Alice",
      nonStrikerName: "A2",
      bowlerName: "Bob",
      runsScored: 4,
      extraType: "NONE",
      extraRuns: 0,
      wicket: false,
      dismissalType: "NONE",
      shotAngleDeg: 45,
      shotDistanceNorm: 0.9,
      powerplay: true,
      commentary: "Crisp cover drive for four!",
      createdAt: pastDate(2000),
    })
  );

  // Non-scorer cannot tamper with match or balls
  await assertFails(
    bobDb.collection("matches").doc("match_1").update({
      teamARuns: 99,
      updatedAt: pastDate(1000),
    })
  );

  // Mark match COMPLETED
  await assertSucceeds(
    aliceDb.collection("matches").doc("match_1").update({
      status: "COMPLETED",
      teamARuns: 150,
      resultText: "Emerald Strikers won by 15 runs",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock blocks score tampering even by scorer
  await assertFails(
    aliceDb.collection("matches").doc("match_1").update({
      teamARuns: 200,
      updatedAt: pastDate(500),
    })
  );

  // Post-match AI summary & stream URL remain updatable by scorer
  await assertSucceeds(
    aliceDb.collection("matches").doc("match_1").update({
      aiSummary: "Emerald Strikers defended 150 clinically.",
      streamUrl: "https://example.com/live.m3u8",
      updatedAt: pastDate(500),
    })
  );
});

test("Tournaments, standings, community posts & moderation reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tour_1").set({
      name: "Emerald Premier Cup",
      format: "ROUND_ROBIN",
      oversPerMatch: 20,
      powerplayOvers: 6,
      organizerUid: ALICE_UID,
      organizerName: "Alice",
      teamIds: ["team_a", "team_b"],
      teamNames: ["Emerald Strikers", "Obsidian Royals"],
      rulesText: "2 pts per win",
      bannerTheme: "EMERALD",
      status: "ONGOING",
      publicVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tour_1").collection("standings").doc("team_a").set({
      tournamentId: "tour_1",
      teamId: "team_a",
      teamName: "Emerald Strikers",
      organizerUid: ALICE_UID,
      played: 1,
      won: 1,
      lost: 0,
      tied: 0,
      noResult: 0,
      points: 2,
      runsFor: 150,
      ballsFaced: 120,
      runsAgainst: 135,
      ballsBowled: 120,
      nrr: 0.75,
      publicVisible: true,
      updatedAt: ts,
    })
  );

  // Community post + comment + report
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorUid: ALICE_UID,
      authorName: "Alice",
      content: "What a finish at Central Turf!",
      postType: "UPDATE",
      likesCount: 0,
      commentsCount: 0,
      hidden: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorUid: BOB_UID,
      authorName: "Bob",
      text: "Great bowling spell!",
      createdAt: pastDate(1000),
    })
  );

  // Report can only be created with status OPEN
  await assertFails(
    bobDb.collection("reports").doc("rep_1").set({
      reporterUid: BOB_UID,
      reporterName: "Bob",
      targetType: "POST",
      targetId: "post_1",
      reason: "Spam",
      status: "RESOLVED",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("reports").doc("rep_1").set({
      reporterUid: BOB_UID,
      reporterName: "Bob",
      targetType: "POST",
      targetId: "post_1",
      reason: "Spam test report",
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
