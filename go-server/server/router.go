package server

import (
	//	"fmt"
	"net"
	//	"os"
)

type Broadcast struct {
	ClientId string
	Message  string
	Beacon   string
}

type NearbyClient struct {
	Id     string
	Conn   net.Conn
	Active []string
}

type Router struct {
	Clients map[string]*NearbyClient
}

func InitNearbyClient(id string, conn net.Conn) *NearbyClient {
	return &NearbyClient{Id: id, Conn: conn, Active: make([]string, 0)}
}

func InitRouter() *Router {
	return &Router{Clients: make(map[string]*NearbyClient)}
}
