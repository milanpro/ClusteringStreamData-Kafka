import React, { useEffect, useState } from "react";
import Chart from "chart.js";
import "./App.css";
import SockJS from "sockjs-client";
import * as Stomp from "@stomp/stompjs";
import { KafkaPointEvent, KafkaClusterCellEvent, KafkaClusterCellDeleteEvent, KafkaClustersEvent } from "./KafkaClusteringTypes";
import ClusterControl from "./ClusterControl";

let scatterChart: Chart | undefined = undefined;

let pointBuffer: any[] = [];
let clusterCellBuffer: KafkaClusterCellEvent[] = [];
const dataMaxSize = 500;
const dataBatchSize = dataMaxSize / 20;

const App: React.FC = () => {
  const [canvasRef, setRef] = useState<null | HTMLCanvasElement>(null);

  useEffect(() => {
    let pointSub: Stomp.StompSubscription;
    let clusterCellSub: Stomp.StompSubscription;
    let clusterSub: Stomp.StompSubscription;
    const client = new Stomp.Client({
      brokerURL: "/live",
      webSocketFactory: () => {
        return new SockJS("/live");
      },
      onConnect: () => {
        pointSub = client.subscribe("/topic/points", value => {
          if (scatterChart) {
            const pointEvent: KafkaPointEvent = JSON.parse(value.body);
            let pointData = scatterChart.data!.datasets![0].data!;
            if (pointBuffer.length === dataBatchSize) {
              if (pointData.length >= 500) {
                pointData = pointData.slice(
                  dataBatchSize,
                  pointData.length - 1
                );
              }
              scatterChart.data!.datasets![0].data! = (pointData as any).concat(
                pointBuffer
              );
              scatterChart.update({ duration: 0 });
              pointBuffer = [];
            } else {
              pointBuffer.push(pointEvent.value);
            }
          }
        });
        clusterCellSub = client.subscribe(
          "/topic/clustercells",
          value => {
            if (scatterChart) {
              const clusterCellEvent: KafkaClusterCellEvent | KafkaClusterCellDeleteEvent  = JSON.parse(value.body);
              clusterCellBuffer = clusterCellBuffer.filter(value => value.key !== clusterCellEvent.key);
              if (clusterCellEvent.value && clusterCellEvent.value.timelyDensity > 0.8){
                clusterCellBuffer.push(clusterCellEvent)
              }
              scatterChart.data!.datasets![1].data! = clusterCellBuffer.map(e => ({
                x: e.value.seedPoint.x,
                y: e.value.seedPoint.y,
                r: e.value.timelyDensity
              }));
              scatterChart.update({duration: 0});
          }

        }
        );
        clusterSub = client.subscribe("/topic/clusters", (value) => {
          if (scatterChart){
          const clustersEvent: KafkaClustersEvent = JSON.parse(value.body);
          let newData: Chart.ChartPoint[] = []
          let newColor: Chart.ChartColor[] = []
          for (let cluster of clustersEvent.value.clusters) {
            const cells = cluster.cluster;
            newData = newData.concat(cells.map(cell => cell.seedPoint));
            const color = `rgba(${Math.random()},${Math.random()},${Math.random()},1)`;
            newColor = newColor.concat(cells.map(() => color));
          }
          scatterChart.data!.datasets![2].data! = newData;
          scatterChart.data!.datasets![2].backgroundColor = newColor;
          scatterChart.update({duration: 0});
        }
        })
      }
    });
    client.activate();
    return () => {
      if (pointSub) pointSub.unsubscribe();
      if (clusterCellSub) clusterCellSub.unsubscribe();
      if (clusterSub) clusterSub.unsubscribe();
    };
  }, []);

  useEffect(() => {
    if (canvasRef) {
      const ctx = canvasRef.getContext("2d");
      if (ctx) {
        scatterChart = new Chart(ctx, {
          type: "scatter",
          data: {
            datasets: [
              {
                label: "points",
                backgroundColor: "rgba(255, 99, 132, 0.7)",
                data: []
              },
              {
                type: "bubble",
                label: "cluster-cells",
                backgroundColor: "rgba(0,0,0,0)",//"blue",
                data: []
              },
              {
                label: "clusters",
                backgroundColor: [],
                data: []
              }
            ]
          },
          options: {
            responsive: true,
            scales: {
              xAxes: [
                {
                  type: "linear",
                  position: "bottom"
                }
              ]
            }
          }
        });
      }
    }
  }, [canvasRef]);

  return (
    <div style={{ display: "flex",
      flexDirection: "row"}}>
    <div style={{ position: "relative", height: "100vh", width: "80vw" }}>
      <canvas ref={setRef} />
    </div>
    <div style={{width: "20vw"}}>
    <ClusterControl />
    </div>
    </div>
  );
};

export default App;
