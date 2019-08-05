import React, { useEffect, useState } from "react";
import { Button, Input } from 'reactstrap';

const ValueControl = (props: {
  field: string,
  buttonLabel: string,
  defaultVal: string
}) => {
  const [val, setVal] = useState(props.defaultVal);

  useEffect(() => {
    fetch(`/getval?key=${props.field}`)
        .then(res => {
            if (res.ok) {
                return res.text();
            } else {
                throw new Error("Key not found")
            }
        })
        .then(setVal)
        .catch(console.error)
  }, []);

  return (
      <div style={{ display: "flex", flexDirection: "row", margin: "10px", height: 64 }}>
        <Input
        style={{width: "30%", paddingRight: 10, marginRight: 10, height: 64}}
            onChange={e => {
              setVal(e.target.value);
            }}
            value={val}
            type="text"
        />
        <Button
        color="primary"
        style={{width: "70%"}}
            onClick={() => {
              fetch("/setval", {
                method: "POST",
                headers: {
                  "Content-Type": "application/json"
                },
                body: JSON.stringify({ key: props.field, value: val })
              });
            }}
        >
          {props.buttonLabel}
        </Button>
      </div>
  );
};

/**
 * Controls values of clustering algorithm.
 * TODO: Value sanitisation
 */
const ClusterControl = () => {
  return (
      <div style={{marginTop: "30px"}}>
        <ValueControl
            field="p2cc/radius"
            buttonLabel="Change Radius"
            defaultVal="10"
        />
        <ValueControl
            field="p2cc/decay"
            buttonLabel="Change Decay"
            defaultVal="0.898"
        />
        <ValueControl
            field="p2cc/lambda"
            buttonLabel="Change Lambda"
            defaultVal="1"
        />
        <ValueControl field="cc2c/xi" buttonLabel="Change XI" defaultVal="0" />
        <ValueControl field="cc2c/tau" buttonLabel="Change Tau" defaultVal="10" />
        <ValueControl
            field="gen/pointDelay"
            buttonLabel="Change Point Delay"
            defaultVal="100"
        />
        <ValueControl
            field="gen/cluster1x"
            buttonLabel="Move Cluster 3 on x-axis"
            defaultVal="90.0"
        />
      </div>
  );
};
export default ClusterControl;
