import {NineLineRequestType} from "../types/NineLineRequestType.ts";
import axios, {HttpStatusCode} from 'axios';
import {RequestAssignmentType} from "../types/RequestAssignmentType.ts";
import {ResponderType} from "../types/ResponderType.ts";

export const submitNineLineRequest = async (newNineLine: NineLineRequestType): Promise<NineLineRequestType> => {
    if (newNineLine.id !== null) {
        return Promise.reject(new Error());
    } else if (newNineLine.location === null) {
        return Promise.reject(new Error());
    }
    try {
        const response = await axios.post("api/v1/medevac", newNineLine, {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            }
        });

        if (response.status !== 201) {
            return Promise.reject(new Error());
        }

        return response.data;
    } catch (error) {
        return Promise.reject(new Error());
    }
};

export const getAssignedNineLineRequests = async (): Promise<NineLineRequestType[]> => {
    try {
        const response = await axios.get("api/v1/medevac")
        if (response.status !== 200) {
            return Promise.reject(new Error())
        }
        return response.data
    } catch (error) {
        return Promise.reject(new Error())
    }
}

export const getAllNineLineRequests = async (): Promise<NineLineRequestType[]> => {
    try {
        const response = await axios.get("api/v1/medevac/all")
        if (response.status !== 200) {
            return Promise.reject(new Error())
        }
        return response.data
    } catch (error) {
        return Promise.reject(new Error())
    }
}

export const updateNineLineRequests = async (requestToCompletes: NineLineRequestType[]): Promise<NineLineRequestType[]> => {
    try {
        const response = await axios.patch("api/v1/medevac", requestToCompletes, {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            }
        });

        if (response.status !== 200) {
            return Promise.reject(new Error());
        }

        return response.data;
    } catch (error) {
        return Promise.reject(new Error());
    }
}

export const postRequestAssignment = async (requestToCompletes: RequestAssignmentType[]): Promise<RequestAssignmentType[]> => {
    try {
        const response = await axios.post("/api/v1/assignment", requestToCompletes, {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            }
        });

        if (response.status !== 201) {
            return Promise.reject(new Error());
        }

        return response.data;
    } catch (error) {
        return Promise.reject(new Error());
    }
}

export const getNineLineRequestsById = async (): Promise<NineLineRequestType> => {
    return Promise.reject();
}

export const getAssignments = async (): Promise<RequestAssignmentType[]> => {
    try {
        const response = await axios.get("/api/v1/assignment")

        if (response.status !== 200)
            return Promise.reject(new Error())

        return response.data
    } catch (error) {
        return Promise.reject(new Error())
    }
}

export const getResponders = async (): Promise<ResponderType[]> => {
    try {
        const response = await axios.get("/api/v1/assignment/responders")

        if (response.status !== 200)
            return Promise.reject(new Error())

        return response.data
    } catch (error) {
        return Promise.reject(new Error())
    }
}

export const getIsSessionValid = async (): Promise<boolean> => {
    try {
        await axios.get("/api/v1/validateSession")
        return true
    } catch {
        return false
    }
}

export const loginUser = async (username: string, password: string): Promise<boolean> => {
    return axios.post("/api/v1/loginUser", {
        username: username,
        password: password
    }, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then((res) => {
        return res.status === HttpStatusCode.Ok
    });
}

export const logoutUser = async (): Promise<boolean> => {
    return axios.post("/api/v1/logoutUser", {}).then(() => {
        return true;
    }).catch(()=> {
        return false
    });
}