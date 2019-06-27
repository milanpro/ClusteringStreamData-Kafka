import React, { useEffect } from 'react';
import logo from './logo.svg';
import './App.css';
import SockJS from "sockjs-client";
import * as Stomp from "@stomp/stompjs";

const App: React.FC = () => {
  useEffect(() => {
    const client = new Stomp.Client({
      brokerURL: "/live",
      webSocketFactory: () => {
        return new SockJS("/live");
      },
      onConnect: () => {
        const pointSub = client.subscribe("/topic/points", (value) => {
            console.log(JSON.parse(value.body));
            pointSub.unsubscribe()
        })
        const clusterCellSub = client.subscribe("/topic/clustercells", (value) => {
            console.log(JSON.parse(value.body));
            clusterCellSub.unsubscribe()
        })
        // const clusterSub = client.subscribe("/topic/clusters", (value) => {
        //     console.log(JSON.parse(value.body));
        //     clusterSub.unsubscribe()
        // })
      }
    })
    client.activate()
  }, [])

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.tsx</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default App;
