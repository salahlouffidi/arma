import XCTest

/// Menu → round 1 → play for about a minute with the joystick; fails if the app stops.
final class GameplaySmokeTests: XCTestCase {
    func testPlayRoundOneMinute() {
        let app = XCUIApplication()
        app.launch()
        sleep(12) // intro video, then menu
        shot(app, "1-menu")

        // Compose testTags are exposed as accessibility identifiers on iOS.
        tap(app, ["menu_play_button"])
        sleep(2)
        shot(app, "2-rounds")
        tap(app, ["round_card_1"])
        sleep(2)
        tap(app, ["play_solo_card", "start_solo_button"], required: false)
        sleep(1)
        tap(app, ["start_solo_button"], required: false)
        sleep(6) // countdown
        shot(app, "3-gameplay")

        let pad = app.coordinate(withNormalizedOffset: CGVector(dx: 0.14, dy: 0.75))
        let moves: [CGVector] = [.init(dx: 110, dy: 0), .init(dx: 0, dy: -100), .init(dx: -110, dy: 0), .init(dx: 0, dy: 100)]
        for i in 0..<36 {
            pad.press(forDuration: 0.05, thenDragTo: pad.withOffset(moves[i % moves.count]),
                      withVelocity: .slow, thenHoldForDuration: 1.0)
            XCTAssertEqual(app.state, .runningForeground, "app stopped after \(i) joystick moves")
            if i == 12 { shot(app, "4-playing") }
        }
        shot(app, "5-end")
        XCTAssertEqual(app.state, .runningForeground)
    }

    private func tap(_ app: XCUIApplication, _ ids: [String], required: Bool = true) {
        for id in ids {
            let e = app.descendants(matching: .any).matching(identifier: id).firstMatch
            if e.waitForExistence(timeout: 4) { e.tap(); return }
        }
        if required {
            for line in app.debugDescription.split(separator: "\n").prefix(60) { print("TREE \(line)") }
            XCTFail("none of \(ids) found")
        }
    }

    private func shot(_ app: XCUIApplication, _ name: String) {
        let a = XCTAttachment(screenshot: app.screenshot())
        a.name = name
        a.lifetime = .keepAlways
        add(a)
    }
}
