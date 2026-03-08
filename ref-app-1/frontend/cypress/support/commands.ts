import '@testing-library/cypress/add-commands.js'
import {nationalities, nbcs, NineLineRequestType} from "../../src/types/NineLineRequestType";

declare global {
    namespace Cypress {
        interface Chainable {
            login(): Chainable<void>

            authenticateAsSuper(): Chainable<void>

            authenticateAsResponder(): Chainable<void>

            createRequest(requestData?: NineLineRequestType): Chainable<void>

            goToResponderPage(): Chainable<void>

            goToDispatcherPage(): Chainable<void>
        }
    }
}


Cypress.Commands.add('login', () => {
    cy.visit('/');
});
Cypress.Commands.add('createRequest', (requestData?: NineLineRequestType) => {
    cy.login();
    if (!requestData) {
        cy.get('#mgrs').type("56J MS 80443 25375")
        cy.get('#callsign').type("922929/Arrow/6")
        cy.get('[aria-label="Hoist"] > .MuiButtonBase-root > .PrivateSwitchBase-input').click()
        cy.get('[aria-label="Panel"] > .MuiButtonBase-root > .PrivateSwitchBase-input').click()
        cy.get('#ambulatorypatient').click().get('[aria-label="2"]').click()
    } else {
        cy.get('#mgrs').type(requestData.location)
        cy.get('#callsign').type(requestData.callsign)
        cy.get('#patientnumber').click().get(`[aria-label="${requestData.patientnumber}"]`).click()
        cy.get('#precedence').click().get(`[aria-label="${requestData.patientnumber}"]`).click()
        for (const requestedEquipment of requestData.specialEquipment) {
            cy.get(`[aria-label="${requestedEquipment}"] > .MuiButtonBase-root > .PrivateSwitchBase-input`).click()
        }
        cy.get('#litterpatient').click().get(`[aria-label="${requestData.litterpatient}"]`).click()
        cy.get('#ambulatorypatient').click().get(`[aria-label="${requestData.ambulatorypatient}"]`).click()
        cy.get('#security').click().get(`[aria-label="${requestData.security}"]`).click()
        for (const markingMethod of requestData.markingMethod) {
            cy.get(`[aria-label="${markingMethod}"] > .MuiButtonBase-root > .PrivateSwitchBase-input`).click()
        }
        cy.get('#nationality').click().get(`[aria-label="${nationalities[requestData.nationality]}"]`).click()
        cy.get('#nbc').click().get(`[aria-label="${nbcs[requestData.nbc]}"]`).click()
    }
    cy.get('#submit').click();
});
Cypress.Commands.add('goToResponderPage', () => {
    cy.visit('/responder')
})

Cypress.Commands.add('goToDispatcherPage', () => {
    cy.visit('/dispatcher');
})

Cypress.Commands.add('authenticateAsResponder', () => {
    cy.visit('/login')
    cy.get('#userNameInput').type("Batman")
    cy.get('#passwordInput').type("Password")
    cy.get('#submitForm').click().wait(500)
})

Cypress.Commands.add('authenticateAsSuper', () => {
    cy.visit('/login')
    cy.get('#userNameInput').type("Darthvader")
    cy.get('#passwordInput').type("Password")
    cy.get('#submitForm').click().wait(500)
})