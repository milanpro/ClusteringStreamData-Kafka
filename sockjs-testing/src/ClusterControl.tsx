import React, { useEffect, useState } from "react";

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
      <div style={{ display: "flex", flexDirection: "row", margin: "10px" }}>
        <input
            onChange={e => {
              setVal(e.target.value);
            }}
            value={val}
            type="text"
        />
        <button
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
        </button>
      </div>
  );
};

const ClusterControl = () => {
  return (
      <div style={{marginTop: "50px"}}>
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
      </div>
  );
};
export default ClusterControl;
