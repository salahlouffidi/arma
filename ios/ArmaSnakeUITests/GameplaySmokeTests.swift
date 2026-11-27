import XCTest

/// Starts a round and plays for a while with the joystick; fails if the app stops.
final class GameplaySmokeTests: XCTestCase {
    func testPlayRoundOneMinute() {
        let app = XCUIApplication()
        app.launch()
        XCUIDevice.shared.orientation = .landscapeLeft
        sleep(12) // intro video, then menu

        let play = app.buttons["PLAY"].firstMatch
        if play.waitForExistence(timeout: 5) { play.tap() }
        else { app.coordinate(withNormalizedOffset: CGVector(dx: 0.75, dy: 0.38)).tap() } // PLAY button
        sleep(5)
        attachScreenshot(app, "after-play")

        // Joystick: drag around the lower-left area for ~60 s.
        let center = app.coordinate(withNormalizedOffset: CGVector(dx: 0.16, dy: 0.72))
        let targets: [CGVector] = [.init(dx: 120, dy: 0), .init(dx: 0, dy: -110), .init(dx: -120, dy: 0), .init(dx: 0, dy: 110)]
        for i in 0..<30 {
            let t = targets[i % targets.count]
            center.press(forDuration: 0.05, thenDragTo: center.withOffset(t), withVelocity: .slow, thenHoldForDuration: 1.2)
            XCTAssertEqual(app.state, .runningForeground, "app stopped after \(i) moves")
            if i == 10 { attachScreenshot(app, "playing") }
        }
        attachScreenshot(app, "end")
        XCTAssertEqual(app.state, .runningForeground)
    }

    private func attachScreenshot(_ app: XCUIApplication, _ name: String) {
        let a = XCTAttachment(screenshot: app.screenshot())
        a.name = name
        a.lifetime = .keepAlways
        add(a)
    }
}
