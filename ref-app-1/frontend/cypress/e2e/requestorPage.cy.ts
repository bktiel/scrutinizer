describe('template spec', () => {
    it('goes to requester page and sees requester button', () => {
        cy.login();
        cy.get('.MuiToolbar-root').findByText(/requester/i).should('be.visible');
    })
    it('should navigate to the requester page and fill out all the inputs needed for submission, then submit, with no errors', () => {
        cy.createRequest();
        cy.findByText(/Request Submitted. A dispatcher will contact you soon/i).should('be.visible');
    });
})