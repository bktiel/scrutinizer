import * as api from "../../services/api.ts";
import {server} from "../mocks/node.ts";
import {sampleResponse, sampleResponses} from "../mocks/handlers.ts";
import {http, HttpResponse} from "msw";
import {afterEach, it} from "vitest";
import {RequestAssignmentType} from "../../types/RequestAssignmentType.ts";
import {NineLineRequestType} from "../../types/NineLineRequestType.ts";
import {ResponderType} from "../../types/ResponderType.ts";

describe('api tests', () => {
    beforeAll(() => server.listen());
    beforeEach(() => server.resetHandlers());
    afterAll(() => server.close());
    afterEach(()=>{
        vi.resetAllMocks()
    })

    it('should make a post request to endpoint when a 9-line request is submitted', async () => {
        server.use(http.post('/api/v1/medevac', async () => {
            return new HttpResponse(JSON.stringify(sampleResponse), {status: 201});
        }),);
        expect(await api.submitNineLineRequest(sampleResponse)).toEqual(sampleResponse);
    });

    it('should reject when invalid response is received', async () => {
        server.use(
            http.post("/api/v1/medevac", async () => new HttpResponse(null, {status: 500}))
        );

        await expect(api.submitNineLineRequest(sampleResponse)).rejects.toThrowError();
    });

    it('should reject invalid request with id specified', async () => {
        server.use(http.post('/api/v1/medevac', async () => {
            return new HttpResponse(JSON.stringify(sampleResponse), {status: 201});
        }),);
        const result = api.submitNineLineRequest({id: 1} as NineLineRequestType);
        await expect(result).rejects.toThrowError();
    });
    it('should make a get request to endpoint when responses specific to a logged in responder desired', async () => {
        server.use(
            http.get("/api/v1/medevac", async () => new HttpResponse(JSON.stringify(sampleResponses), {status: 200}))
        );
        const result = await api.getAssignedNineLineRequests();
        expect(result).toStrictEqual(sampleResponses);
    });
    it('should make a get request to endpoint when all requests is desired', async () => {
        server.use(
            http.get("/api/v1/medevac/all", async () => new HttpResponse(JSON.stringify(sampleResponses), {status: 200}))
        );

        const result = await api.getAllNineLineRequests();
        expect(result).toStrictEqual(sampleResponses);
    });
    it('should send get request to endpoint when all responders desired', async () => {
        const sampleResponders:ResponderType[]=[
            {
                id:1,
                callsign:"Dad"
            },
            {
                id:2,
                callsign:"Paul"
            },
        ]
        server.use(
            http.get("/api/v1/assignment/responders", async () => new HttpResponse(JSON.stringify(sampleResponders), {status: 200}))
        );

        const result = await api.getResponders();
        expect(result).toStrictEqual(sampleResponders);
    });
    it('should make a patch request to endpoint when update to a 9-line request is submitted ', async () => {
        const sample = [{...sampleResponse, status: "Complete", id: 1}, {...sampleResponse, status: "Complete", id: 2}];
        server.use(http.patch('/api/v1/medevac', async () => {
            return new HttpResponse(JSON.stringify(sample), {status: 200});
        }),);
        const result = await api.updateNineLineRequests(sampleResponses);
        expect(result).toStrictEqual(sample);
    });

    it('should send assignment request when dispatcher page handles submit.', async () => {
        const sample: RequestAssignmentType[] = [{
            requestId: 1,
            responder: 1,
        }];

        server.use(
            http.post("/api/v1/assignment", async () => new HttpResponse(JSON.stringify(sample), {status: 201})));

        const result = await api.postRequestAssignment(sample);
        expect(result).toStrictEqual(sample);
    });

    it('should retrieve assignment datas', async () => {
        const sample: RequestAssignmentType[] = [
            {
                requestId: 1,
                responder: 1,
            }
        ];

        server.use(
            http.get("/api/v1/assignment", async () => HttpResponse.json(sample))
        );

        const result = await api.getAssignments();
        expect(result).toStrictEqual(sample);
    });
});

it('should send a login request when the login page handles login attempt', async () => {
    const username = "Batman";
    const password = "Password";

    server.use(
        http.post("/api/v1/loginUser", async () => new HttpResponse(null, {status: 200}))
    );

    const loginResult = await api.loginUser(username, password);
    expect(loginResult).toStrictEqual(true)
});