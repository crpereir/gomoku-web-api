import * as React from 'react';

export type CustomLabelProps = {
  key: string;
  for: string;
  name: string;
  inputId: string;
  inputName: string;
  inputType: string;
  inputValue: string;
};

export type CustomFormProps = {
  handleSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  disabled: boolean;
  labels: Array<CustomLabelProps>;
  buttonName: string;
  handleChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
};

export function CustomForm({ handleSubmit, disabled, labels, buttonName, handleChange }: CustomFormProps) {
  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={disabled}>
        {labels.map(label => (
          <div key={label.key}>
            <label htmlFor={label.for}>{label.name}</label>
            <input
              id={label.inputId}
              type={label.inputType}
              name={label.inputName}
              value={label.inputValue}
              required
              onChange={handleChange}
            />
          </div>
        ))}
        <div>
          <button type="submit">{buttonName}</button>
        </div>
      </fieldset>
    </form>
  );
}
