import api from "../axios";
import type { Customer, CustomerCreationRequest } from "../../types/customer";

export async function findByClientId(clientId: string): Promise<Customer> {

    const response = await api.get<Customer>(
        "/api/customers/" + clientId
    );

    return response.data;
}

export async function findOrCreate(
    payload: CustomerCreationRequest
): Promise<Customer> {

    const response = await api.post<Customer>(
        "/api/customers/find-or-create",
        payload
    );

    return response.data;
}